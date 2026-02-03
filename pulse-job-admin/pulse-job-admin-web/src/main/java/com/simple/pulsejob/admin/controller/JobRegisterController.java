package com.simple.pulsejob.admin.controller;

import java.util.List;

import com.simple.pulsejob.admin.business.service.IJobRegisterService;
import com.simple.pulsejob.admin.common.model.base.ResponseResult;
import com.simple.pulsejob.admin.common.model.param.JobInfoParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/job")
@RequiredArgsConstructor
public class JobRegisterController {

    private final IJobRegisterService jobRegisterService;

    @PostMapping("/register")
    public ResponseResult<Void> registerJob(@RequestBody List<JobInfoParam> jobRegisterParams) {
        try {
            jobRegisterService.registerJob(jobRegisterParams);
            return ResponseResult.ok();
        } catch (Exception e) {
            log.error("注册任务失败", e);
            return ResponseResult.error("注册任务失败: " + e.getMessage());
        }
    }

    @PostMapping("/trigger")
    public ResponseResult<Void> triggerJob() {
        try {
            jobRegisterService.triggerJob();
            return ResponseResult.ok();
        } catch (Exception e) {
            log.error("触发任务失败", e);
            return ResponseResult.error("触发任务失败: " + e.getMessage());
        }
    }
}
