package com.simple.pulsejob.admin.controller;

import java.util.List;
import com.simple.pulsejob.admin.business.service.IJobRegisterService;
import com.simple.pulsejob.admin.common.model.param.JobInfoParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/job")
@RequiredArgsConstructor
public class JobRegisterController {

    private final IJobRegisterService jobRegisterService;

    @GetMapping("/register")
    public void registerJob(@RequestBody List<JobInfoParam> jobRegisterParams) {
        jobRegisterService.registerJob(jobRegisterParams);
    }

    @GetMapping("/trigger")
    public void triggerJob() {
        jobRegisterService.triggerJob();
    }

}
