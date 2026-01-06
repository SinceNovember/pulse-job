package com.simple.pulsejob.admin.controller;

import com.simple.pulsejob.admin.business.service.IJobLogService;
import com.simple.pulsejob.admin.common.model.base.ResponseResult;
import com.simple.pulsejob.admin.common.model.entity.JobLog;
import com.simple.pulsejob.admin.common.model.enums.LogLevelEnum;
import com.simple.pulsejob.admin.scheduler.log.JobLogStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务日志接口.
 * 
 * <p>提供日志查询、统计、清理等功能</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/job-log")
@RequiredArgsConstructor
public class JobLogController {

    private final IJobLogService jobLogService;
    private final JobLogStorageService jobLogStorageService;

    /**
     * 根据调用ID查询日志
     *
     * @param instanceId 调用ID
     * @return 日志列表
     */
    @GetMapping("/invoke/{instanceId}")
    public ResponseResult<List<JobLog>> findByInstanceId(@PathVariable Long instanceId) {
        List<JobLog> logs = jobLogService.findByInstanceId(instanceId);
        return ResponseResult.ok(logs);
    }

    /**
     * 分页查询指定调用ID的日志
     *
     * @param instanceId 调用ID
     * @param page     页码（从0开始）
     * @param size     每页大小
     * @return 日志分页结果
     */
    @GetMapping("/invoke/{instanceId}/page")
    public ResponseResult<Page<JobLog>> findByInstanceIdPage(
            @PathVariable Long instanceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "sequence"));
        Page<JobLog> logs = jobLogService.findByInstanceId(instanceId, pageRequest);
        return ResponseResult.ok(logs);
    }

    /**
     * 根据任务ID查询日志
     *
     * @param jobId 任务ID
     * @return 日志列表
     */
    @GetMapping("/job/{jobId}")
    public ResponseResult<List<JobLog>> findByJobId(@PathVariable Integer jobId) {
        List<JobLog> logs = jobLogService.findByJobId(jobId);
        return ResponseResult.ok(logs);
    }

    /**
     * 分页查询指定任务ID的日志
     *
     * @param jobId 任务ID
     * @param page  页码
     * @param size  每页大小
     * @return 日志分页结果
     */
    @GetMapping("/job/{jobId}/page")
    public ResponseResult<Page<JobLog>> findByJobIdPage(
            @PathVariable Integer jobId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<JobLog> logs = jobLogService.findByJobId(jobId, pageRequest);
        return ResponseResult.ok(logs);
    }

    /**
     * 查询指定调用ID的错误日志
     *
     * @param instanceId 调用ID
     * @return 错误日志列表
     */
    @GetMapping("/invoke/{instanceId}/errors")
    public ResponseResult<List<JobLog>> findErrorLogs(@PathVariable Long instanceId) {
        List<JobLog> logs = jobLogService.findErrorLogs(instanceId);
        return ResponseResult.ok(logs);
    }

    /**
     * 综合条件搜索日志
     *
     * @param instanceId     调用ID（可选）
     * @param jobId        任务ID（可选）
     * @param executorName 执行器名称（可选）
     * @param logLevel     日志级别（可选）
     * @param startTime    开始时间
     * @param endTime      结束时间
     * @param page         页码
     * @param size         每页大小
     * @return 日志分页结果
     */
    @GetMapping("/search")
    public ResponseResult<Page<JobLog>> search(
            @RequestParam(required = false) Long instanceId,
            @RequestParam(required = false) Integer jobId,
            @RequestParam(required = false) String executorName,
            @RequestParam(required = false) LogLevelEnum logLevel,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<JobLog> logs = jobLogService.search(instanceId, jobId, executorName, logLevel, startTime, endTime, pageRequest);
        return ResponseResult.ok(logs);
    }

    /**
     * 统计指定调用ID的日志数量
     *
     * @param instanceId 调用ID
     * @return 日志数量
     */
    @GetMapping("/invoke/{instanceId}/count")
    public ResponseResult<Long> countByInstanceId(@PathVariable Long instanceId) {
        long count = jobLogService.countByInstanceId(instanceId);
        return ResponseResult.ok(count);
    }

    /**
     * 统计指定任务ID的日志数量
     *
     * @param jobId 任务ID
     * @return 日志数量
     */
    @GetMapping("/job/{jobId}/count")
    public ResponseResult<Long> countByJobId(@PathVariable Integer jobId) {
        long count = jobLogService.countByJobId(jobId);
        return ResponseResult.ok(count);
    }

    /**
     * 删除指定调用ID的日志
     *
     * @param instanceId 调用ID
     * @return 删除的记录数
     */
    @DeleteMapping("/invoke/{instanceId}")
    public ResponseResult<Integer> deleteByInstanceId(@PathVariable Long instanceId) {
        int count = jobLogService.deleteByInstanceId(instanceId);
        return ResponseResult.ok(count);
    }

    /**
     * 删除指定任务ID的日志
     *
     * @param jobId 任务ID
     * @return 删除的记录数
     */
    @DeleteMapping("/job/{jobId}")
    public ResponseResult<Integer> deleteByJobId(@PathVariable Integer jobId) {
        int count = jobLogService.deleteByJobId(jobId);
        return ResponseResult.ok(count);
    }

    /**
     * 清理过期日志
     *
     * @param retentionDays 保留天数
     * @return 删除的记录数
     */
    @PostMapping("/clean")
    public ResponseResult<Integer> cleanExpiredLogs(
            @RequestParam(defaultValue = "30") int retentionDays) {
        int count = jobLogService.cleanExpiredLogs(retentionDays);
        return ResponseResult.ok(count);
    }

    /**
     * 获取日志存储统计信息
     *
     * @return 存储统计
     */
    @GetMapping("/storage/stats")
    public ResponseResult<JobLogStorageService.StorageStats> getStorageStats() {
        return ResponseResult.ok(jobLogStorageService.getStats());
    }

    /**
     * 强制刷新日志缓冲区
     */
    @PostMapping("/storage/flush")
    public ResponseResult<Void> flushStorage() {
        jobLogStorageService.flush();
        return ResponseResult.ok(null);
    }
}

