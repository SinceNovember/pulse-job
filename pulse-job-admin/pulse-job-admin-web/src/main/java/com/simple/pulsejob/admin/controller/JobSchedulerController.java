package com.simple.pulsejob.admin.controller;

import com.simple.pulsejob.admin.common.model.base.ResponseResult;
import com.simple.pulsejob.admin.scheduler.JSchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/scheduler")
@RequiredArgsConstructor
public class JobSchedulerController {

    private final JSchedulerService jobSchedulerService;

    /**
     * 取消任务调度
     */
    @PostMapping("/cancel/{jobId}")
    public ResponseResult<Void> cancelJob(@PathVariable Integer jobId) {
        try {
            jobSchedulerService.cancelJob(jobId);
            return ResponseResult.ok();
        } catch (Exception e) {
            log.error("取消任务调度失败: {}", jobId, e);
            return ResponseResult.error("取消任务调度失败: " + e.getMessage());
        }
    }

    /**
     * 获取已调度的任务数量
     */
    @GetMapping("/count")
    public ResponseResult<Integer> getScheduledJobCount() {
        try {
            int count = jobSchedulerService.getScheduledJobCount();
            return ResponseResult.ok(count);
        } catch (Exception e) {
            log.error("获取调度任务数量失败", e);
            return ResponseResult.error("获取调度任务数量失败: " + e.getMessage());
        }
    }

    /**
     * 手动触发任务调度
     */
    @PostMapping("/trigger")
    public ResponseResult<Void> triggerScheduling() {
        try {
            jobSchedulerService.scheduleJobs();
            return ResponseResult.ok();
        } catch (Exception e) {
            log.error("手动触发任务调度失败", e);
            return ResponseResult.error("手动触发任务调度失败: " + e.getMessage());
        }
    }
} 