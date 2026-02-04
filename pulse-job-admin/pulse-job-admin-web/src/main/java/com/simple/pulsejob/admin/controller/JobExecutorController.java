package com.simple.pulsejob.admin.controller;

import com.simple.pulsejob.admin.business.service.IJobExecutorService;
import com.simple.pulsejob.admin.common.model.base.PageResult;
import com.simple.pulsejob.admin.common.model.base.ResponseResult;
import com.simple.pulsejob.admin.common.model.entity.JobExecutor;
import com.simple.pulsejob.admin.common.model.enums.RegisterTypeEnum;
import com.simple.pulsejob.admin.common.model.param.JobExecutorParam;
import com.simple.pulsejob.admin.common.model.param.JobExecutorQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/jobExecutor")
public class JobExecutorController {

    private final IJobExecutorService jobExecutorService;

    /**
     * 分页查询执行器列表
     */
    @GetMapping("/page")
    public ResponseResult<PageResult<JobExecutor>> pageJobExecutors(JobExecutorQuery query) {
        try {
            PageResult<JobExecutor> result = jobExecutorService.pageJobExecutors(query);
            return ResponseResult.ok(result);
        } catch (Exception e) {
            log.error("分页查询执行器失败", e);
            return ResponseResult.error("分页查询执行器失败: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseResult<Void> createJobExecutor(@RequestBody JobExecutorParam jobExecutorParam) {
        try {
            jobExecutorService.addJobExecutor(jobExecutorParam);
            return ResponseResult.created(null);
        } catch (Exception e) {
            log.error("创建执行器失败", e);
            return ResponseResult.error("创建执行器失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseResult<JobExecutor> getJobExecutorById(@PathVariable Integer id) {
        return ResponseResult.of(jobExecutorService.getJobExecutorById(id), "执行器不存在");
    }

    @GetMapping("/name/{executorName}")
    public ResponseResult<JobExecutor> getJobExecutorByName(@PathVariable String executorName) {
        return ResponseResult.of(jobExecutorService.getJobExecutorByName(executorName), "执行器不存在");
    }

    @GetMapping
    public ResponseResult<List<JobExecutor>> getAllJobExecutors() {
        return ResponseResult.ok(jobExecutorService.getAllJobExecutors());
    }

    @GetMapping("/register-type/{registerType}")
    public ResponseResult<List<JobExecutor>> getJobExecutorsByRegisterType(@PathVariable RegisterTypeEnum registerType) {
        return ResponseResult.ok(jobExecutorService.getJobExecutorsByRegisterType(registerType));
    }

    @PutMapping("/{id}")
    public ResponseResult<JobExecutor> updateJobExecutor(@PathVariable("id") Integer id, @RequestBody JobExecutor jobExecutor) {
        if (!id.equals(jobExecutor.getId())) {
            return ResponseResult.badRequest("ID不匹配");
        }
        try {
            JobExecutor updatedJobExecutor = jobExecutorService.updateJobExecutor(jobExecutor);
            return ResponseResult.ok(updatedJobExecutor);
        } catch (IllegalArgumentException e) {
            return ResponseResult.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("更新执行器失败", e);
            return ResponseResult.error("更新执行器失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseResult<Void> deleteJobExecutor(@PathVariable("id") Integer id) {
        try {
            jobExecutorService.deleteJobExecutor(id);
            return ResponseResult.ok();
        } catch (Exception e) {
            log.error("删除执行器失败", e);
            return ResponseResult.error("删除执行器失败: " + e.getMessage());
        }
    }

    @PostMapping("/batch")
    public ResponseResult<Void> batchCreateJobExecutors(@RequestBody List<JobExecutorParam> jobExecutorParams) {
        try {
            jobExecutorService.batchAddJobExecutors(jobExecutorParams);
            return ResponseResult.created(null);
        } catch (IllegalArgumentException e) {
            return ResponseResult.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("批量创建执行器失败", e);
            return ResponseResult.error("批量创建执行器失败: " + e.getMessage());
        }
    }
}
