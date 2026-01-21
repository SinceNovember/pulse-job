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
 * 定时任务调度器（优化版）.
 *
 * <p>整合了 xxl-job 和 PowerJob 的优秀设计：</p>
 * <ul>
 *   <li>【PowerJob】扫描时立即更新下次执行时间，不等任务执行完</li>
 *   <li>【xxl-job】过期任务分类处理：过期太久跳过、刚过期立即执行</li>
 *   <li>【xxl-job】时间轮只存 jobId，触发时重新获取最新 JobInfo</li>
 *   <li>【优化】正在执行的任务不重复调度</li>
 *   <li>【优化】高频任务执行后立即重调度</li>
 *   <li>【优化】长延迟任务不放入时间轮，等待扫描</li>
 * </ul>
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

    /** 扫描间隔（毫秒） */
    @Value("${pulse.job.admin.scheduler.query-interval:5000}")
    private long queryInterval;

    /** 预读时间窗口（秒），建议为 2 × 扫描间隔 */
    @Value("${pulse.job.admin.scheduler.query-window:10}")
    private int queryWindow;

    /** 时间轮调度阈值（秒），延迟小于此值才放入时间轮 */
    @Value("${pulse.job.admin.scheduler.wheel-threshold-seconds:#{${pulse.job.admin.scheduler.query-window:10}}}")
    private int wheelThresholdSeconds;

    /** 过期容忍时间（秒），超过此时间的过期任务跳过本次执行 */
    @Value("${pulse.job.admin.scheduler.misfire-threshold-seconds:10}")
    private int misfireThresholdSeconds;

    // ==================== 内部状态 ====================

    /** 运行状态 */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /** 已调度到时间轮的任务（jobId -> Timeout） */
    private final ConcurrentHashMap<Integer, Timeout> scheduledJobs = new ConcurrentHashMap<>();

    /** 正在执行中的任务，防止并发执行 */
    private final Set<Integer> runningJobs = ConcurrentHashMap.newKeySet();

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

        schedulerExecutor = Executors.newSingleThreadScheduledExecutor(
                new JNamedThreadFactory("cron-scheduler", true));

        jobExecutor = new ThreadPoolExecutor(
                4, 16,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                new JNamedThreadFactory("cron-job-executor", true),
                new ThreadPoolExecutor.CallerRunsPolicy());

        log.info("CronJobScheduler 初始化完成: queryInterval={}ms, queryWindow={}s, wheelThreshold={}s, misfireThreshold={}s",
                queryInterval, queryWindow, wheelThresholdSeconds, misfireThresholdSeconds);
    }

    public void start() {
        if (!enabled) {
            log.warn("CronJobScheduler 已禁用，无法启动");
            return;
        }

        if (running.compareAndSet(false, true)) {
            log.info("CronJobScheduler 启动");
            schedulerExecutor.scheduleWithFixedDelay(
                    this::scanAndScheduleJobs,
                    0,
                    queryInterval,
                    TimeUnit.MILLISECONDS);
        }
    }

    @PreDestroy
    public void stop() {
        if (running.compareAndSet(true, false)) {
            log.info("CronJobScheduler 停止中...");

            scheduledJobs.forEach((jobId, timeout) -> {
                timeout.cancel();
            });
            scheduledJobs.clear();

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
     * 扫描并调度任务（参考 xxl-job + PowerJob）
     */
    private void scanAndScheduleJobs() {
        if (!running.get()) {
            return;
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endTime = now.plusSeconds(queryWindow);

            // 查询时间窗口内的启用任务（包括已过期的）
            // 注意：这里 startTime 用 now 减去 misfireThreshold，以便捕获刚过期的任务
            LocalDateTime startTime = now.minusSeconds(misfireThresholdSeconds);
            List<JobInfo> jobs = jobInfoMapper.findJobsToExecute(startTime, endTime, 1);

            if (jobs.isEmpty()) {
                log.debug("没有需要调度的任务，时间窗口: {} ~ {}", startTime, endTime);
                return;
            }

            log.debug("扫描到 {} 个待调度任务", jobs.size());

            for (JobInfo job : jobs) {
                try {
                    processJob(job, now);
                } catch (Exception e) {
                    log.error("处理任务 {} 时发生错误", job.getId(), e);
                }
            }

        } catch (Exception e) {
            log.error("扫描任务时发生错误", e);
        }
    }

    /**
     * 处理单个任务（参考 xxl-job 的过期任务处理策略）
     */
    @Transactional
    protected void processJob(JobInfo jobInfo, LocalDateTime now) {
        Integer jobId = jobInfo.getId();

        // 1. 检查是否已暂停
        if (pausedJobs.contains(jobId)) {
            return;
        }

        // 2. 检查是否正在执行中
        if (runningJobs.contains(jobId)) {
            log.debug("任务 {} 正在执行中，跳过本次调度", jobId);
            return;
        }

        // 3. 检查是否已在时间轮中
        if (scheduledJobs.containsKey(jobId)) {
            return;
        }

        // 4. 获取调度策略
        ScheduleStrategy strategy = strategyFactory.getStrategy(jobInfo.getScheduleType());
        if (!strategy.needAutoSchedule()) {
            return;
        }

        LocalDateTime nextExecuteTime = jobInfo.getNextExecuteTime();
        if (nextExecuteTime == null) {
            nextExecuteTime = strategy.calculateNextExecuteTime(jobInfo);
            if (nextExecuteTime == null) {
                return;
            }
        }

        long delayMs = ChronoUnit.MILLIS.between(now, nextExecuteTime);

        // 5. 【xxl-job 策略】过期任务分类处理
        if (delayMs < 0) {
            long overdueMs = -delayMs;
            long misfireThresholdMs = misfireThresholdSeconds * 1000L;

            if (overdueMs > misfireThresholdMs) {
                // 过期太久（超过阈值），跳过本次，直接计算下次执行时间
                log.warn("任务 {} 过期 {}ms > 阈值 {}ms，跳过本次执行，计算下次时间",
                        jobId, overdueMs, misfireThresholdMs);
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               refreshNextExecuteTime(jobInfo, strategy);
                return;
            } else {
                // 刚过期（在阈值内），立即执行
                log.info("任务 {} 过期 {}ms <= 阈值 {}ms，立即执行",
                        jobId, overdueMs, misfireThresholdMs);
                delayMs = 0;
            }
        }

        // 6. 【PowerJob 策略】扫描时立即更新下次执行时间
        // 不等任务执行完，这样下次扫描就能看到新的执行时间
        refreshNextExecuteTime(jobInfo, strategy);

        // 7. 放入时间轮
        scheduleToWheel(jobId, delayMs);
    }

    /**
     * 【PowerJob 优化】扫描时立即计算并更新下次执行时间
     */
    @Transactional
    protected void refreshNextExecuteTime(JobInfo jobInfo, ScheduleStrategy strategy) {
        LocalDateTime nextTime = strategy.calculateNextExecuteTime(jobInfo);
        if (nextTime != null) {
            jobInfoMapper.updateNextExecuteTime(jobInfo.getId(), nextTime, LocalDateTime.now());
            jobInfo.setNextExecuteTime(nextTime);
            log.debug("任务 {} 下次执行时间更新为: {}", jobInfo.getId(), nextTime);
        }
    }

    /**
     * 【xxl-job 优化】时间轮只存 jobId，触发时重新获取最新 JobInfo
     */
    private void scheduleToWheel(Integer jobId, long delayMs) {
        // 如果延迟超过阈值，不放入时间轮
        if (delayMs >= wheelThresholdSeconds * 1000L) {
            log.debug("任务 {} 延迟 {}ms >= 阈值，等待下次扫描", jobId, delayMs);
            return;
        }

        log.info("调度任务到时间轮: jobId={}, delay={}ms", jobId, delayMs);

        // 时间轮只存 jobId
        Timeout timeout = hashedWheelTimer.newTimeout(
                task -> triggerJob(jobId),  // 只传 jobId
                delayMs,
                TimeUnit.MILLISECONDS);

        scheduledJobs.put(jobId, timeout);
    }

    /**
     * 时间轮触发任务
     */
    private void triggerJob(Integer jobId) {
        scheduledJobs.remove(jobId);

        if (!running.get() || pausedJobs.contains(jobId)) {
            return;
        }

        // 【xxl-job 优化】触发时重新从 DB 获取最新的 JobInfo
        jobInfoMapper.findById(jobId).ifPresentOrElse(
                jobInfo -> {
                    // 检查是否正在执行
                    if (runningJobs.contains(jobId)) {
                        log.warn("任务 {} 正在执行中，跳过本次触发", jobId);
                        return;
                    }
                    // 提交到线程池执行
                    jobExecutor.submit(() -> executeJob(jobInfo));
                },
                () -> log.warn("任务 {} 不存在或已删除", jobId)
        );
    }

    /**
     * 执行任务
     */
    private void executeJob(JobInfo jobInfo) {
        Integer jobId = jobInfo.getId();

        // 标记为正在执行
        if (!runningJobs.add(jobId)) {
            log.warn("任务 {} 正在执行中，跳过", jobId);
            return;
        }

        try {
            log.info("开始执行任务: jobId={}, handler={}", jobId, jobInfo.getJobHandler());

            ScheduleConfig config = buildScheduleConfig(jobInfo);
            Object result = invoker.invoke(config);

            log.info("任务执行成功: jobId={}, result={}", jobId, result);
            jobInfo.resetRetryTimes();

        } catch (Throwable e) {
            log.error("任务执行失败: jobId={}", jobId, e);
            handleExecutionError(jobInfo, e);

        } finally {
            // 移除执行标记
            runningJobs.remove(jobId);

            // 【高频任务优化】执行完后检查是否需要立即重调度
            rescheduleIfNeeded(jobInfo);
        }
    }

    /**
     * 【高频任务优化】执行完后检查是否需要立即重调度
     */
    private void rescheduleIfNeeded(JobInfo jobInfo) {
        Integer jobId = jobInfo.getId();

        if (pausedJobs.contains(jobId) || !running.get()) {
            return;
        }

        ScheduleStrategy strategy = strategyFactory.getStrategy(jobInfo.getScheduleType());
        if (!strategy.needAutoSchedule()) {
            return;
        }

        // 获取最新的下次执行时间（扫描时已更新）
        LocalDateTime nextTime = jobInfo.getNextExecuteTime();
        if (nextTime == null) {
            return;
        }

        long delayMs = ChronoUnit.MILLIS.between(LocalDateTime.now(), nextTime);

        // 如果下次执行时间在阈值内，立即放入时间轮
        if (delayMs >= 0 && delayMs < wheelThresholdSeconds * 1000L) {
            if (!scheduledJobs.containsKey(jobId)) {
                log.debug("高频任务 {} 立即重调度，delay={}ms", jobId, delayMs);
                scheduleToWheel(jobId, delayMs);
            }
        }
    }

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
        return config;
    }

    private void handleExecutionError(JobInfo jobInfo, Throwable e) {
        Integer jobId = jobInfo.getId();
        int currentRetry = jobInfo.getRetryTimes();
        int maxRetry = jobInfo.getMaxRetryTimes();

        if (currentRetry < maxRetry) {
            jobInfo.incrementRetryTimes();
            log.warn("任务 {} 执行失败，将进行第 {} 次重试", jobId, jobInfo.getRetryTimes());

            hashedWheelTimer.newTimeout(
                    task -> triggerJob(jobId),
                    30,
                    TimeUnit.SECONDS);
        } else {
            log.error("任务 {} 执行失败，已达到最大重试次数 {}", jobId, maxRetry);
            // TODO: 告警通知
        }
    }

    // ==================== 公开方法 ====================

    public boolean pause(Integer jobId) {
        if (pausedJobs.add(jobId)) {
            Timeout timeout = scheduledJobs.remove(jobId);
            if (timeout != null) {
                timeout.cancel();
            }
            log.info("暂停任务: {}", jobId);
            return true;
        }
        return false;
    }

    public boolean resume(Integer jobId) {
        if (pausedJobs.remove(jobId)) {
            log.info("恢复任务: {}", jobId);
            return true;
        }
        return false;
    }

    public boolean cancel(Integer jobId) {
        Timeout timeout = scheduledJobs.remove(jobId);
        if (timeout != null) {
            timeout.cancel();
            log.info("取消任务调度: {}", jobId);
            return true;
        }
        return false;
    }

    public void triggerNow(Integer jobId) {
        jobInfoMapper.findById(jobId).ifPresent(jobInfo -> {
            log.info("立即触发任务: jobId={}", jobId);
            jobExecutor.submit(() -> executeJob(jobInfo));
        });
    }

    public int getScheduledCount() {
        return scheduledJobs.size();
    }

    public int getRunningCount() {
        return runningJobs.size();
    }

    public boolean isScheduled(Integer jobId) {
        return scheduledJobs.containsKey(jobId);
    }

    public boolean isRunning() {
        return running.get();
    }
}
