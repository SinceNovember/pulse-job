package com.simple.pulsejob.admin.scheduler;

import com.simple.pulsejob.admin.common.model.entity.JobInfo;
import com.simple.pulsejob.admin.common.model.enums.ScheduleTypeEnum;
import com.simple.pulsejob.admin.scheduler.invoker.Invoker;
import com.simple.pulsejob.admin.scheduler.strategy.ScheduleStrategy;
import com.simple.pulsejob.admin.scheduler.strategy.ScheduleStrategyFactory;
import com.simple.pulsejob.admin.scheduler.timer.Timeout;
import com.simple.pulsejob.admin.scheduler.timer.Timer;
import com.simple.pulsejob.admin.scheduler.timer.TimerTask;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 任务调度引擎
 * <p>
 * 负责任务的调度、执行、重试等核心逻辑。
 * 通过 {@link ScheduleStrategyFactory} 获取对应的调度策略来计算下次执行时间。
 * </p>
 *
 * @author pulse
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobScheduleEngine implements JobScheduler {

    private final Timer hashedWheelTimer;
    private final Invoker invoker;
    private final ScheduleStrategyFactory strategyFactory;

    @Resource
    private ThreadPoolExecutor jobExecutor;

    /** 调度器运行状态 */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /** 已暂停的任务集合 */
    private final Set<Long> pausedJobs = ConcurrentHashMap.newKeySet();

    /** 已调度的任务缓存，避免重复调度 */
    private final ConcurrentHashMap<Long, Timeout> scheduledJobs = new ConcurrentHashMap<>();

    // ==================== JobScheduler 接口实现 ====================

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            log.info("调度引擎启动");
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            log.info("调度引擎停止");
            // 取消所有已调度的任务
            scheduledJobs.forEach((jobId, timeout) -> {
                timeout.cancel();
                log.debug("取消任务调度: {}", jobId);
            });
            scheduledJobs.clear();
        }
    }

    @Override
    public void schedule(JobInfo jobInfo) {
        if (!running.get()) {
            log.warn("调度引擎未启动，无法调度任务: {}", jobInfo.getId());
            return;
        }
        doSchedule(jobInfo);
    }

    @Override
    public void trigger() {
//        if (!running.get()) {
//            log.warn("调度引擎未启动，无法触发任务");
//            return;
//        }
        
        try {
            ScheduleConfig config = new ScheduleConfig();
            config.setJobId(1);
            invoker.invoke(config);
        } catch (Throwable e) {
            log.error("触发任务失败: jobId={}", 1, e);
        }
    }

    @Override
    public boolean cancel(Long jobId) {
        Timeout timeout = scheduledJobs.remove(jobId);
        if (timeout != null) {
            timeout.cancel();
            log.info("取消任务调度: {}", jobId);
            return true;
        }
        return false;
    }

    @Override
    public boolean pause(Long jobId) {
        if (pausedJobs.add(jobId)) {
            log.info("暂停任务: {}", jobId);
            // 取消当前调度（如果有）
            cancel(jobId);
            return true;
        }
        return false;
    }

    @Override
    public boolean resume(Long jobId) {
        if (pausedJobs.remove(jobId)) {
            log.info("恢复任务: {}", jobId);
            return true;
        }
        return false;
    }

    @Override
    public int getScheduledCount() {
        return scheduledJobs.size();
    }

    @Override
    public boolean isScheduled(Long jobId) {
        return scheduledJobs.containsKey(jobId);
    }

    // ==================== 内部方法 ====================

    /**
     * 调度单个任务到时间轮
     */
    private void doSchedule(JobInfo jobInfo) {
        Long jobId = Long.valueOf(jobInfo.getId());
        
        try {
            // 获取调度策略
            ScheduleTypeEnum scheduleType = jobInfo.getScheduleType();
            ScheduleStrategy strategy = strategyFactory.getStrategy(scheduleType);

            // API 类型不需要自动调度
            if (!strategy.needAutoSchedule()) {
                log.debug("任务 {} 为 {} 类型，不需要自动调度", jobId, scheduleType);
                return;
            }

            // 检查任务是否已暂停
            if (pausedJobs.contains(jobId)) {
                log.debug("任务 {} 已暂停，跳过调度", jobId);
                return;
            }

            // 检查任务是否已经调度
            if (scheduledJobs.containsKey(jobId)) {
                log.debug("任务 {} 已经调度，跳过", jobId);
                return;
            }

            // 计算延迟时间
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextExecuteTime = jobInfo.getNextExecuteTime();
            
            // 如果下次执行时间为空，使用策略计算
            if (nextExecuteTime == null) {
                nextExecuteTime = strategy.calculateNextExecuteTime(jobInfo);
                if (nextExecuteTime == null) {
                    log.warn("任务 {} 无法计算下次执行时间", jobId);
                    return;
                }
            }

            long delaySeconds = ChronoUnit.SECONDS.between(now, nextExecuteTime);

            if (delaySeconds <= 0) {
                // 任务已经过期，立即执行
                delaySeconds = 0;
            }

            log.info("调度任务: ID={}, Type={}, Handler={}, 延迟={}秒",
                    jobId, scheduleType, jobInfo.getJobHandler(), delaySeconds);

            // 创建定时任务
            Timeout timeout = hashedWheelTimer.newTimeout(
                    task -> executeJob(jobInfo),
                    delaySeconds,
                    TimeUnit.SECONDS
            );

            // 缓存已调度的任务
            scheduledJobs.put(jobId, timeout);

        } catch (Exception e) {
            log.error("调度任务 {} 时发生错误", jobId, e);
        }
    }

    /**
     * 执行任务
     */
    private void executeJob(JobInfo jobInfo) {
        Long jobId = Long.valueOf(jobInfo.getId());
        
        try {
            log.info("开始执行任务: ID={}, Handler={}", jobId, jobInfo.getJobHandler());

            // 从缓存中移除任务
            scheduledJobs.remove(jobId);

            // 检查是否已暂停
            if (pausedJobs.contains(jobId)) {
                log.info("任务 {} 已暂停，跳过执行", jobId);
                return;
            }

            // 提交到线程池执行
            jobExecutor.submit(() -> {
                try {
                    // 通过 Invoker 调用执行器执行任务
                    // TODO: 从 jobInfo 获取 executorId
//                    invoker.invoke(
//                            jobInfo.getExecutorName(),
//                            jobId,
//                            Long.valueOf(jobInfo.getExecutorId()),
//                            jobInfo.getJobHandler(),
//                            jobInfo.getJobParams()
//                    );

                    // 计算下次执行时间
                    calculateNextExecuteTime(jobInfo);

                } catch (Exception e) {
                    log.error("执行任务 {} 时发生错误", jobId, e);
                    handleJobExecutionError(jobInfo, e);
                }
            });

        } catch (Exception e) {
            log.error("准备执行任务 {} 时发生错误", jobId, e);
        }
    }

    /**
     * 计算下次执行时间并重新调度
     */
    private void calculateNextExecuteTime(JobInfo jobInfo) {
        try {
            // 获取调度策略
            ScheduleTypeEnum scheduleType = jobInfo.getScheduleType();
            ScheduleStrategy strategy = strategyFactory.getStrategy(scheduleType);

            // API 类型不需要自动调度
            if (!strategy.needAutoSchedule()) {
                log.debug("任务 {} 为 API 类型，不自动计算下次执行时间", jobInfo.getId());
                return;
            }

            // 使用策略计算下次执行时间
            LocalDateTime nextExecuteTime = strategy.calculateNextExecuteTime(jobInfo);

            if (nextExecuteTime != null) {
                // 更新任务的下次执行时间
                jobInfo.updateNextExecuteTime(nextExecuteTime);
                log.info("任务 {} 下次执行时间更新为: {}", jobInfo.getId(), nextExecuteTime);

                // 重新调度任务
                doSchedule(jobInfo);
            } else {
                log.warn("任务 {} 无法计算下次执行时间，停止调度", jobInfo.getId());
            }

        } catch (Exception e) {
            log.error("计算任务 {} 下次执行时间时发生错误", jobInfo.getId(), e);
        }
    }

    /**
     * 处理任务执行错误
     */
    private void handleJobExecutionError(JobInfo jobInfo, Exception e) {
        try {
            int currentRetryTimes = jobInfo.getRetryTimes();
            int maxRetryTimes = jobInfo.getMaxRetryTimes();

            if (currentRetryTimes < maxRetryTimes) {
                // 还有重试机会
                jobInfo.setRetryTimes(currentRetryTimes + 1);

                log.warn("任务 {} 执行失败，将进行第 {} 次重试", jobInfo.getId(), jobInfo.getRetryTimes());

                // 延迟重试
                hashedWheelTimer.newTimeout(new TimerTask() {
                    @Override
                    public void run(Timeout timeout) throws Exception {
                        executeJob(jobInfo);
                    }
                }, 30, TimeUnit.SECONDS); // 30秒后重试

            } else {
                log.error("任务 {} 执行失败，已达到最大重试次数 {}", jobInfo.getId(), maxRetryTimes);
                // TODO: 可以在这里实现告警通知
            }

        } catch (Exception ex) {
            log.error("处理任务 {} 执行错误时发生异常", jobInfo.getId(), ex);
        }
    }
}

