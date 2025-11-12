package com.simple.pulsejob.admin.scheduler;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import com.simple.pulsejob.admin.common.model.entity.JobInfo;
import com.simple.pulsejob.admin.scheduler.timer.Timeout;
import com.simple.pulsejob.admin.scheduler.timer.Timer;
import com.simple.pulsejob.admin.scheduler.timer.TimerTask;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JSchedulerService {

//    private final IJobInfoService jobInfoService;
    private final Timer hashedWheelTimer;

    @Resource
    private ThreadPoolExecutor jobExecutor;

    // 创建一个定时线程池
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * 已调度的任务缓存，避免重复调度
     */
    private final ConcurrentHashMap<Integer, Timeout> scheduledJobs = new ConcurrentHashMap<>();

    /**
     * 每5秒执行一次，查询即将执行的任务并推送到时间轮
     */
    public void scheduleJobs() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                queryAndExecuteJobs();
            } catch (Exception e) {
                e.printStackTrace(); // 捕获异常避免任务中断
            }
        }, 0, 5, TimeUnit.SECONDS); // 初始延迟0秒，每5秒执行一次

    }

    private void queryAndExecuteJobs() {
        try {
            log.debug("开始查询即将执行的任务...");

            // 查询未来10秒内需要执行的任务
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime endTime = now.plusSeconds(5);

            List<JobInfo> jobsToExecute = new ArrayList<>();
//            List<JobInfo> jobsToExecute = jobInfoService.findJobsToExecute(now, endTime);

            log.info("查询到 {} 个即将执行的任务", jobsToExecute.size());

            for (JobInfo jobInfo : jobsToExecute) {
                scheduleJob(jobInfo);
            }

        } catch (Exception e) {
            log.error("调度任务时发生错误", e);
        }
    }

    /**
     * 调度单个任务到时间轮
     */
    private void scheduleJob(JobInfo jobInfo) {
        try {
            // 检查任务是否已经调度
            if (scheduledJobs.containsKey(jobInfo.getId())) {
                log.debug("任务 {} 已经调度，跳过", jobInfo.getId());
                return;
            }

            // 计算延迟时间
            LocalDateTime now = LocalDateTime.now();
            long delaySeconds = ChronoUnit.SECONDS.between(now, jobInfo.getNextExecuteTime());
            
            if (delaySeconds <= 0) {
                // 任务已经过期，立即执行
                delaySeconds = 0;
            }

            log.info("调度任务: ID={}, Handler={}, 延迟={}秒", 
                    jobInfo.getId(), jobInfo.getJobHandler(), delaySeconds);

            // 创建定时任务
            Timeout timeout = hashedWheelTimer.newTimeout(task -> executeJob(jobInfo)
                , delaySeconds, TimeUnit.SECONDS);

            // 缓存已调度的任务
            scheduledJobs.put(jobInfo.getId(), timeout);
            
        } catch (Exception e) {
            log.error("调度任务 {} 时发生错误", jobInfo.getId(), e);
        }
    }

    /**
     * 执行任务
     */
    private void executeJob(JobInfo jobInfo) {
        try {
            log.info("开始执行任务: ID={}, Handler={}", jobInfo.getId(), jobInfo.getJobHandler());
            
            // 从缓存中移除任务
            scheduledJobs.remove(jobInfo.getId());
            
            // 更新任务执行状态
            LocalDateTime now = LocalDateTime.now();
//            jobInfoService.updateExecutionStatus(jobInfo.getId(), now, jobInfo.getRetryTimes());
            
            // 提交到线程池执行
            jobExecutor.submit(() -> {
                try {
                    // 这里应该调用具体的任务执行逻辑
                    // 可以通过RPC调用执行器来执行任务
                    executeJobTask(jobInfo);
                    
                    // 计算下次执行时间
                    calculateNextExecuteTime(jobInfo);
                    
                } catch (Exception e) {
                    log.error("执行任务 {} 时发生错误", jobInfo.getId(), e);
                    handleJobExecutionError(jobInfo, e);
                }
            });
            
        } catch (Exception e) {
            log.error("准备执行任务 {} 时发生错误", jobInfo.getId(), e);
        }
    }

    /**
     * 执行具体的任务逻辑
     */
    private void executeJobTask(JobInfo jobInfo) {
        // TODO: 这里应该实现具体的任务执行逻辑
        // 可以通过RPC调用执行器来执行任务
        log.info("执行任务逻辑: {}", jobInfo.getJobHandler());
        
        // 模拟任务执行
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 计算下次执行时间
     */
    private void calculateNextExecuteTime(JobInfo jobInfo) {
        try {
            LocalDateTime nextExecuteTime = null;
            
            // 根据调度类型计算下次执行时间
            switch (jobInfo.getScheduleType()) {
                case CRON:
                    // TODO: 解析CRON表达式计算下次执行时间
                    nextExecuteTime = LocalDateTime.now().plusSeconds(60); // 临时实现
                    break;
                default:
                    log.warn("不支持的调度类型: {}", jobInfo.getScheduleType());
                    return;
            }
            
            if (nextExecuteTime != null) {
//                jobInfoService.updateNextExecuteTime(jobInfo.getId(), nextExecuteTime);
                log.info("任务 {} 下次执行时间更新为: {}", jobInfo.getId(), nextExecuteTime);
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
//                jobInfoService.updateExecutionStatus(jobInfo.getId(), LocalDateTime.now(), jobInfo.getRetryTimes());
                
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

    /**
     * 取消任务调度
     */
    public void cancelJob(Integer jobId) {
        Timeout timeout = scheduledJobs.remove(jobId);
        if (timeout != null) {
            timeout.cancel();
            log.info("取消任务调度: {}", jobId);
        }
    }

    /**
     * 获取已调度的任务数量
     */
    public int getScheduledJobCount() {
        return scheduledJobs.size();
    }
} 