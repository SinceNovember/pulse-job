package com.simple.pulsejob.admin.controller;

import java.util.List;

import com.simple.pulsejob.admin.business.service.IJobInfoService;
import com.simple.pulsejob.admin.common.model.base.PageResult;
import com.simple.pulsejob.admin.common.model.base.ResponseResult;
import com.simple.pulsejob.admin.common.model.entity.JobInfo;
import com.simple.pulsejob.admin.common.model.param.JobInfoParam;
import com.simple.pulsejob.admin.common.model.param.JobInfoQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/jobInfo")
public class JobInfoController {

    private final IJobInfoService jobInfoService;

    @PostMapping
    public ResponseResult<Void> addJobInfo(@RequestBody JobInfoParam jobInfoParam) {
        try {
            jobInfoService.addJobInfo(jobInfoParam);
            return ResponseResult.created(null);
        } catch (Exception e) {
            log.error("添加任务失败", e);
            return ResponseResult.error("添加任务失败: " + e.getMessage());
        }
    }

    @PostMapping("/batch")
    public ResponseResult<Void> batchAddJobInfos(@RequestBody List<JobInfoParam> jobInfoParams) {
        try {
            jobInfoService.batchAddJobInfos(jobInfoParams);
            return ResponseResult.created(null);
        } catch (Exception e) {
            log.error("批量添加任务失败", e);
            return ResponseResult.error("批量添加任务失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseResult<JobInfo> getJobInfoById(@PathVariable("id") Integer id) {
        return ResponseResult.of(jobInfoService.getJobInfoById(id), "任务不存在");
    }

    @GetMapping
    public ResponseResult<List<JobInfo>> getAllJobInfos() {
        return ResponseResult.ok(jobInfoService.getAllJobInfos());
    }

    @GetMapping("/handler/{jobHandler}")
    public ResponseResult<List<JobInfo>> getJobInfosByHandler(@PathVariable("jobHandler") String jobHandler) {
        return ResponseResult.ok(jobInfoService.getJobInfosByHandler(jobHandler));
    }

    @PutMapping("/{id}")
    public ResponseResult<JobInfo> updateJobInfo(@PathVariable("id") Integer id, @RequestBody JobInfo jobInfo) {
        if (!id.equals(jobInfo.getId())) {
            return ResponseResult.badRequest("ID不匹配");
        }
        try {
            JobInfo updatedJobInfo = jobInfoService.updateJobInfo(jobInfo);
            return ResponseResult.ok(updatedJobInfo);
        } catch (Exception e) {
            log.error("更新任务失败", e);
            return ResponseResult.error("更新任务失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseResult<Void> deleteJobInfo(@PathVariable("id") Integer id) {
        try {
            jobInfoService.deleteJobInfo(id);
            return ResponseResult.ok();
        } catch (Exception e) {
            log.error("删除任务失败", e);
            return ResponseResult.error("删除任务失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询任务列表
     */
    @GetMapping("/page")
    public ResponseResult<PageResult<JobInfo>> pageJobInfos(JobInfoQuery query) {
        try {
            PageResult<JobInfo> pageResult = jobInfoService.pageJobInfos(query);
            return ResponseResult.ok(pageResult);
        } catch (Exception e) {
            log.error("分页查询任务失败", e);
            return ResponseResult.error("分页查询任务失败: " + e.getMessage());
        }
    }

    /**
     * 启用任务
     */
    @PostMapping("/{id}/enable")
    public ResponseResult<Void> enableJob(@PathVariable("id") Integer id) {
        try {
            jobInfoService.setJobEnabled(id, true);
            return ResponseResult.ok();
        } catch (Exception e) {
            log.error("启用任务失败", e);
            return ResponseResult.error("启用任务失败: " + e.getMessage());
        }
    }

    /**
     * 禁用任务
     */
    @PostMapping("/{id}/disable")
    public ResponseResult<Void> disableJob(@PathVariable("id") Integer id) {
        try {
            jobInfoService.setJobEnabled(id, false);
            return ResponseResult.ok();
        } catch (Exception e) {
            log.error("禁用任务失败", e);
            return ResponseResult.error("禁用任务失败: " + e.getMessage());
        }
    }
}
