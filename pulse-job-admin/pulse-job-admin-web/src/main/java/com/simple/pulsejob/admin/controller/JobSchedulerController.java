package com.simple.pulsejob.admin.controller;

import com.simple.pulsejob.admin.common.model.base.ResponseResult;
import com.simple.pulsejob.admin.scheduler.JobScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/scheduler")
@RequiredArgsConstructor
public class JobSchedulerController {

    private final JobScheduler jobScheduler;

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
     */
    @PostMapping("/trigger")
    public ResponseResult<Void> trigger(
            @RequestParam String executorName,
            @RequestParam Long jobId,
            @RequestParam Long executorId,
            @RequestParam String handlerName,
            @RequestParam(required = false) String params) {
        try {
            jobScheduler.trigger(executorName, jobId, executorId, handlerName, params);
            return ResponseResult.ok();
        } catch (Exception e) {
            log.error("手动触发任务失败: jobId={}", jobId, e);
            return ResponseResult.error("手动触发任务失败: " + e.getMessage());
        }
    }

    /**
     * 取消任务调度
     */
    @PostMapping("/cancel/{jobId}")
    public ResponseResult<Boolean> cancelJob(@PathVariable Long jobId) {
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
    public ResponseResult<Boolean> pauseJob(@PathVariable Long jobId) {
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
}
