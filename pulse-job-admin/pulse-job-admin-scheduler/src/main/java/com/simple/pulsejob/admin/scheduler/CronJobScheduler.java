package com.simple.pulsejob.admin.scheduler;

import com.simple.pulsejob.admin.common.model.entity.JobInfo;
import com.simple.pulsejob.admin.common.model.enums.ScheduleTypeEnum;
import com.simple.pulsejob.admin.persistence.mapper.JobInfoMapper;
import com.simple.pulsejob.admin.scheduler.invoker.Invoker;
import com.simple.pulsejob.admin.scheduler.strategy.ScheduleStrategy;
import com.simple.pulsejob.admin.scheduler.strategy.ScheduleStrategyFactory;
import com.simple.pulsejob.admin.scheduler.timer.Timeout;
import com.simple.pulsejob.admin.scheduler.timer.Timer;
import com.simple.pulsejob.common.concurrent.JNamedThreadFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 定时任务调度器.
 *
 * <p>定期从数据库拉取即将执行的任务，提交到时间轮中执行。</p>
 *
 * <p>工作流程：</p>
 * <ol>
 *   <li>定期查询数据库中 next_execute_time 在时间窗口内的启用任务</li>
 *   <li>计算延迟时间，将任务提交到时间轮</li>
 *   <li>任务执行完成后，计算下次执行时间并更新数据库</li>
 *   <li>如果是周期性任务，重新调度</li>
 * </ol>
 *
 * @author pulse
 */
@Slf4j
@Component
public class CronJobScheduler {

    private final Timer hashedWheelTimer;
    private final Invoker invoker;
    private final ScheduleStrategyFactory strategyFactory;
    private final JobInfoMapper jobInfoMapper;

    // ==================== 配置项 ====================

    @Value("${pulse.job.admin.scheduler.enabled:true}")
    private boolean enabled;

    @Value("${pulse.job.admin.scheduler.query-interval:5000}")
    private long queryInterval;

    @Value("${pulse.job.admin.scheduler.query-window:10}")
    private int queryWindow;

    @Value("${pulse.job.admin.scheduler.pre-read-seconds:5}")
    private int preReadSeconds;

    // ==================== 内部状态 ====================

    /** 运行状态 */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /** 已调度的任务（jobId -> Timeout），避免重复调度 */
    private final ConcurrentHashMap<Integer, Timeout> scheduledJobs = new ConcurrentHashMap<>();

    /** 已暂停的任务 */
    private final Set<Integer> pausedJobs = ConcurrentHashMap.newKeySet();

    /** 调度线程池 */
    private ScheduledExecutorService schedulerExecutor;

    /** 任务执行线程池 */
    private ExecutorService jobExecutor;

    public CronJobScheduler(Timer hashedWheelTimer, 
                            Invoker invoker,
                            ScheduleStrategyFactory strategyFactory,
                            JobInfoMapper jobInfoMapper) {
        this.hashedWheelTimer = hashedWheelTimer;
        this.invoker = invoker;
        this.strategyFactory = strategyFactory;
        this.jobInfoMapper = jobInfoMapper;
    }

    // ==================== 生命周期 ====================

    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info("CronJobScheduler 已禁用");
            return;
        }

        // 初始化线程池
        schedulerExecutor = Executors.newSingleThreadScheduledExecutor(
                new JNamedThreadFactory("cron-scheduler", true));

        jobExecutor = new ThreadPoolExecutor(
                4, 16,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                new JNamedThreadFactory("cron-job-executor", true),
                new ThreadPoolExecutor.CallerRunsPolicy());

        log.info("CronJobScheduler 初始化完成，queryInterval={}ms, queryWindow={}s",
                queryInterval, queryWindow);
    }

    /**
     * 启动调度器
     */
    public void start() {
        if (!enabled) {
            log.warn("CronJobScheduler 已禁用，无法启动");
            return;
        }

        if (running.compareAndSet(false, true)) {
            log.info("CronJobScheduler 启动");

            // 启动定时查询任务
            schedulerExecutor.scheduleWithFixedDelay(
                    this::scanAndScheduleJobs,
                    0,
                    queryInterval,
                    TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 停止调度器
     */
    @PreDestroy
    public void stop() {
        if (running.compareAndSet(true, false)) {
            log.info("CronJobScheduler 停止中...");

            // 取消所有已调度的任务
            scheduledJobs.forEach((jobId, timeout) -> {
                timeout.cancel();
                log.debug("取消任务调度: jobId={}", jobId);
            });
            scheduledJobs.clear();

            // 关闭线程池
            if (schedulerExecutor != null) {
                schedulerExecutor.shutdown();
            }
            if (jobExecutor != null) {
                jobExecutor.shutdown();
                try {
                    if (!jobExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                        jobExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    jobExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }

            log.info("CronJobScheduler 已停止");
        }
    }

    // ==================== 核心调度逻辑 ====================

    /**
     * 扫描并调度任务
     */
    private void scanAndScheduleJobs() {
        if (!running.get()) {
            return;
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endTime = now.plusSeconds(queryWindow);

            // 查询时间窗口内的启用任务
            List<JobInfo> jobs = jobInfoMapper.findJobsToExecute(now, endTime, 1);

            if (jobs.isEmpty()) {
                log.debug("没有需要调度的任务，时间窗口: {} ~ {}", now, endTime);
                return;
            }

            log.info("扫描到 {} 个待调度任务，时间窗口: {} ~ {}", jobs.size(), now, endTime);

            for (JobInfo job : jobs) {
                scheduleJob(job);
            }

        } catch (Exception e) {
            log.error("扫描任务时发生错误", e);
        }
    }

    /**
     * 调度单个任务到时间轮
     */
    private void scheduleJob(JobInfo jobInfo) {
        Integer jobId = jobInfo.getId();

        try {
            // 检查任务是否已暂停
            if (pausedJobs.contains(jobId)) {
                log.debug("任务 {} 已暂停，跳过调度", jobId);
                return;
            }

            // 检查任务是否已调度
            if (scheduledJobs.containsKey(jobId)) {
                log.debug("任务 {} 已在调度队列中，跳过", jobId);
                return;
            }

            // 获取调度策略
            ScheduleTypeEnum scheduleType = jobInfo.getScheduleType();
            ScheduleStrategy strategy = strategyFactory.getStrategy(scheduleType);

            // API 类型不需要自动调度
            if (!strategy.needAutoSchedule()) {
                log.debug("任务 {} 为 {} 类型，不需要自动调度", jobId, scheduleType);
                return;
            }

            // 计算延迟时间
            LocalDateTime nextExecuteTime = jobInfo.getNextExecuteTime();
            if (nextExecuteTime == null) {
                nextExecuteTime = strategy.calculateNextExecuteTime(jobInfo);
                if (nextExecuteTime == null) {
                    log.warn("任务 {} 无法计算下次执行时间", jobId);
                    return;
                }
            }

            long delayMs = ChronoUnit.MILLIS.between(LocalDateTime.now(), nextExecuteTime);
            if (delayMs < 0) {
                delayMs = 0; // 已过期，立即执行
            }

            log.info("调度任务: jobId={}, type={}, handler={}, nextExecuteTime={}, delay={}ms",
                    jobId, scheduleType, jobInfo.getJobHandler(), nextExecuteTime, delayMs);

            // 提交到时间轮
            Timeout timeout = hashedWheelTimer.newTimeout(
                    task -> executeJob(jobInfo),
                    delayMs,
                    TimeUnit.MILLISECONDS);

            scheduledJobs.put(jobId, timeout);

        } catch (Exception e) {
            log.error("调度任务 {} 时发生错误", jobId, e);
        }
    }

    /**
     * 执行任务
     */
    private void executeJob(JobInfo jobInfo) {
        Integer jobId = jobInfo.getId();

        // 从调度队列移除
        scheduledJobs.remove(jobId);

        // 检查是否已暂停
        if (pausedJobs.contains(jobId)) {
            log.info("任务 {} 已暂停，跳过执行", jobId);
            return;
        }

        // 检查是否仍在运行
        if (!running.get()) {
            log.info("调度器已停止，跳过执行任务 {}", jobId);
            return;
        }

        // 提交到线程池异步执行
        jobExecutor.submit(() -> doExecuteJob(jobInfo));
    }

    /**
     * 实际执行任务
     */
    private void doExecuteJob(JobInfo jobInfo) {
        Integer jobId = jobInfo.getId();
        String jobHandler = jobInfo.getJobHandler();

        log.info("开始执行任务: jobId={}, handler={}", jobId, jobHandler);

        try {
            // 构建调度配置
            ScheduleConfig config = buildScheduleConfig(jobInfo);

            // 通过 Invoker 执行任务
            Object result = invoker.invoke(config);

            log.info("任务执行成功: jobId={}, handler={}, result={}", jobId, jobHandler, result);

            // 重置重试次数
            jobInfo.resetRetryTimes();

        } catch (Throwable e) {
            log.error("任务执行失败: jobId={}, handler={}", jobId, jobHandler, e);

            // 处理执行错误
            handleExecutionError(jobInfo, e);
        } finally {
            // 计算并更新下次执行时间
            updateNextExecuteTime(jobInfo);
        }
    }

    /**
     * 构建调度配置
     */
    private ScheduleConfig buildScheduleConfig(JobInfo jobInfo) {
        ScheduleConfig config = new ScheduleConfig();
        config.setJobId(jobInfo.getId());
        config.setJobHandler(jobInfo.getJobHandler());
        config.setJobParams(jobInfo.getJobParams());
        config.setScheduleType(jobInfo.getScheduleType());
        config.setScheduleExpression(jobInfo.getScheduleRate());
        config.setDispatchType(jobInfo.getDispatchType());
        config.setLoadBalanceType(jobInfo.getLoadBalanceType());
        config.setSerializerType(jobInfo.getSerializerType());
        config.setRetries(jobInfo.getMaxRetryTimes() != null ? jobInfo.getMaxRetryTimes() : 1);
        config.setSync(false);
        return config;
    }

    /**
     * 处理执行错误
     */
    private void handleExecutionError(JobInfo jobInfo, Throwable e) {
        Integer jobId = jobInfo.getId();
        int currentRetry = jobInfo.getRetryTimes();
        int maxRetry = jobInfo.getMaxRetryTimes();

        if (currentRetry < maxRetry) {
            jobInfo.incrementRetryTimes();
            log.warn("任务 {} 执行失败，将进行第 {} 次重试", jobId, jobInfo.getRetryTimes());

            // 延迟重试（30秒后）
            hashedWheelTimer.newTimeout(
                    task -> executeJob(jobInfo),
                    30,
                    TimeUnit.SECONDS);
        } else {
            log.error("任务 {} 执行失败，已达到最大重试次数 {}", jobId, maxRetry);
            // TODO: 告警通知
        }
    }

    /**
     * 更新下次执行时间并重新调度
     */
    @Transactional
    public void updateNextExecuteTime(JobInfo jobInfo) {
        Integer jobId = jobInfo.getId();

        try {
            // 获取调度策略
            ScheduleStrategy strategy = strategyFactory.getStrategy(jobInfo.getScheduleType());

            // API 类型不需要自动调度
            if (!strategy.needAutoSchedule()) {
                log.debug("任务 {} 为 API 类型，不自动调度", jobId);
                return;
            }

            // 计算下次执行时间
            LocalDateTime nextExecuteTime = strategy.calculateNextExecuteTime(jobInfo);

            if (nextExecuteTime != null) {
                // 更新数据库
                jobInfoMapper.updateNextExecuteTime(jobId, nextExecuteTime, LocalDateTime.now());

                // 更新内存中的对象
                jobInfo.setNextExecuteTime(nextExecuteTime);
                jobInfo.setLastExecuteTime(LocalDateTime.now());

                log.info("任务 {} 下次执行时间更新为: {}", jobId, nextExecuteTime);

            } else {
                log.warn("任务 {} 无法计算下次执行时间，停止自动调度", jobId);
            }

        } catch (Exception e) {
            log.error("更新任务 {} 下次执行时间时发生错误", jobId, e);
        }
    }

    // ==================== 公开方法 ====================

    /**
     * 暂停任务
     */
    public boolean pause(Integer jobId) {
        if (pausedJobs.add(jobId)) {
            // 取消当前调度
            Timeout timeout = scheduledJobs.remove(jobId);
            if (timeout != null) {
                timeout.cancel();
            }
            log.info("暂停任务: {}", jobId);
            return true;
        }
        return false;
    }

    /**
     * 恢复任务
     */
    public boolean resume(Integer jobId) {
        if (pausedJobs.remove(jobId)) {
            log.info("恢复任务: {}", jobId);
            // 下次扫描时会自动重新调度
            return true;
        }
        return false;
    }

    /**
     * 取消任务调度
     */
    public boolean cancel(Integer jobId) {
        Timeout timeout = scheduledJobs.remove(jobId);
        if (timeout != null) {
            timeout.cancel();
            log.info("取消任务调度: {}", jobId);
            return true;
        }
        return false;
    }

    /**
     * 立即触发任务（不等待调度时间）
     */
    public void triggerNow(Integer jobId) {
        jobInfoMapper.findById(jobId).ifPresent(jobInfo -> {
            log.info("立即触发任务: jobId={}", jobId);
            jobExecutor.submit(() -> doExecuteJob(jobInfo));
        });
    }

    /**
     * 获取已调度任务数量
     */
    public int getScheduledCount() {
        return scheduledJobs.size();
    }

    /**
     * 检查任务是否已调度
     */
    public boolean isScheduled(Integer jobId) {
        return scheduledJobs.containsKey(jobId);
    }

    /**
     * 检查调度器是否运行中
     */
    public boolean isRunning() {
        return running.get();
    }
}
