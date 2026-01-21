package com.simple.pulsejob.admin.controller;

import com.simple.pulsejob.admin.business.service.IJobTriggerService;
import com.simple.pulsejob.admin.common.model.base.ResponseResult;
import com.simple.pulsejob.admin.scheduler.CronJobScheduler;
import com.simple.pulsejob.admin.scheduler.JobScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/scheduler")
@RequiredArgsConstructor
public class JobSchedulerController {

    private final JobScheduler jobScheduler;

    private final IJobTriggerService jobTriggerService;

    private final CronJobScheduler cronJobScheduler;
    /**
     * 启动调度引擎
     */
    @PostMapping("/start")
    public ResponseResult<Void> start() {
        try {
            jobScheduler.start();
            return ResponseResult.ok();
        } catch (Exception e) {
            log.error("启动调度引擎失败", e);
            return ResponseResult.error("启动调度引擎失败: " + e.getMessage());
        }
    }

    /**
     * 停止调度引擎
     */
    @PostMapping("/stop")
    public ResponseResult<Void> stop() {
        try {
            jobScheduler.stop();
            return ResponseResult.ok();
        } catch (Exception e) {
            log.error("停止调度引擎失败", e);
            return ResponseResult.error("停止调度引擎失败: " + e.getMessage());
        }
    }

    /**
     * 手动触发任务
     *
     * @param jobId  任务ID
     * @param params 执行参数（可选，传入则覆盖任务配置的参数）
     */
    @PostMapping("/trigger/{jobId}")
    public ResponseResult<Void> trigger(@PathVariable("jobId") Integer jobId,
                                        @RequestParam(value = "params", required = false) String params) {
        try {
            if (params != null && !params.isEmpty()) {
                jobTriggerService.trigger(jobId, params);
            } else {
                jobTriggerService.trigger(jobId);
            }
            return ResponseResult.ok();
        } catch (IllegalArgumentException e) {
            log.warn("手动触发任务参数错误: jobId={}, error={}", jobId, e.getMessage());
            return ResponseResult.error(e.getMessage());
        } catch (IllegalStateException e) {
            log.warn("手动触发任务状态错误: jobId={}, error={}", jobId, e.getMessage());
            return ResponseResult.error(e.getMessage());
        } catch (Exception e) {
            log.error("手动触发任务失败: jobId={}", jobId, e);
            return ResponseResult.error("手动触发任务失败: " + e.getMessage());
        }
    }

    /**
     * 取消任务调度
     */
    @PostMapping("/cancel/{jobId}")
    public ResponseResult<Boolean> cancelJob(@PathVariable("jobId") Long jobId) {
        try {
            boolean result = jobScheduler.cancel(jobId);
            return ResponseResult.ok(result);
        } catch (Exception e) {
            log.error("取消任务调度失败: {}", jobId, e);
            return ResponseResult.error("取消任务调度失败: " + e.getMessage());
        }
    }

    /**
     * 暂停任务
     */
    @PostMapping("/pause/{jobId}")
    public ResponseResult<Boolean> pauseJob(@PathVariable("jobId") Long jobId) {
        try {
            boolean result = jobScheduler.pause(jobId);
            return ResponseResult.ok(result);
        } catch (Exception e) {
            log.error("暂停任务失败: {}", jobId, e);
            return ResponseResult.error("暂停任务失败: " + e.getMessage());
        }
    }

    /**
     * 恢复任务
     */
    @PostMapping("/resume/{jobId}")
    public ResponseResult<Boolean> resumeJob(@PathVariable Long jobId) {
        try {
            boolean result = jobScheduler.resume(jobId);
            return ResponseResult.ok(result);
        } catch (Exception e) {
            log.error("恢复任务失败: {}", jobId, e);
            return ResponseResult.error("恢复任务失败: " + e.getMessage());
        }
    }

    /**
     * 获取已调度的任务数量
     */
    @GetMapping("/count")
    public ResponseResult<Integer> getScheduledJobCount() {
        try {
            int count = jobScheduler.getScheduledCount();
            return ResponseResult.ok(count);
        } catch (Exception e) {
            log.error("获取调度任务数量失败", e);
            return ResponseResult.error("获取调度任务数量失败: " + e.getMessage());
        }
    }

    /**
     * 检查任务是否已调度
     */
    @GetMapping("/isScheduled/{jobId}")
    public ResponseResult<Boolean> isScheduled(@PathVariable Long jobId) {
        try {
            boolean result = jobScheduler.isScheduled(jobId);
            return ResponseResult.ok(result);
        } catch (Exception e) {
            log.error("检查任务调度状态失败: {}", jobId, e);
            return ResponseResult.error("检查任务调度状态失败: " + e.getMessage());
        }
    }

    // ==================== Cron 调度器接口 ====================

    /**
     * 启动 Cron 定时调度器
     */
    @PostMapping("/cron/start")
    public ResponseResult<Void> startCronScheduler() {
        try {
            cronJobScheduler.start();
            return ResponseResult.ok();
        } catch (Exception e) {
            log.error("启动 Cron 调度器失败", e);
            return ResponseResult.error("启动 Cron 调度器失败: " + e.getMessage());
        }
    }

    /**
     * 停止 Cron 定时调度器
     */
    @PostMapping("/cron/stop")
    public ResponseResult<Void> stopCronScheduler() {
        try {
            cronJobScheduler.stop();
            return ResponseResult.ok();
        } catch (Exception e) {
            log.error("停止 Cron 调度器失败", e);
            return ResponseResult.error("停止 Cron 调度器失败: " + e.getMessage());
        }
    }

    /**
     * 获取 Cron 调度器状态
     */
    @GetMapping("/cron/status")
    public ResponseResult<Map<String, Object>> getCronSchedulerStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("running", cronJobScheduler.isRunning());
            status.put("scheduledCount", cronJobScheduler.getScheduledCount());
            status.put("runningCount", cronJobScheduler.getRunningCount());
            return ResponseResult.ok(status);
        } catch (Exception e) {
            log.error("获取 Cron 调度器状态失败", e);
            return ResponseResult.error("获取 Cron 调度器状态失败: " + e.getMessage());
        }
    }

    /**
     * 暂停定时任务
     */
    @PostMapping("/cron/pause/{jobId}")
    public ResponseResult<Boolean> pauseCronJob(@PathVariable("jobId") Integer jobId) {
        try {
            boolean result = cronJobScheduler.pause(jobId);
            return ResponseResult.ok(result);
        } catch (Exception e) {
            log.error("暂停定时任务失败: {}", jobId, e);
            return ResponseResult.error("暂停定时任务失败: " + e.getMessage());
        }
    }

    /**
     * 恢复定时任务
     */
    @PostMapping("/cron/resume/{jobId}")
    public ResponseResult<Boolean> resumeCronJob(@PathVariable("jobId") Integer jobId) {
        try {
            boolean result = cronJobScheduler.resume(jobId);
            return ResponseResult.ok(result);
        } catch (Exception e) {
            log.error("恢复定时任务失败: {}", jobId, e);
            return ResponseResult.error("恢复定时任务失败: " + e.getMessage());
        }
    }

    /**
     * 立即执行定时任务（不等待调度时间）
     */
    @PostMapping("/cron/triggerNow/{jobId}")
    public ResponseResult<Void> triggerCronJobNow(@PathVariable("jobId") Integer jobId) {
        try {
            cronJobScheduler.triggerNow(jobId);
            return ResponseResult.ok();
        } catch (Exception e) {
            log.error("立即执行定时任务失败: {}", jobId, e);
            return ResponseResult.error("立即执行定时任务失败: " + e.getMessage());
        }
    }
}
