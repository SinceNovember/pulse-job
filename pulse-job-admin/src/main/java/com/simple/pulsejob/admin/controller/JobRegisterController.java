package com.simple.pulsejob.admin.controller;

import java.util.List;
import com.simple.pulsejob.admin.model.param.JobRegisterParam;
import com.simple.pulsejob.admin.service.IJobRegisterService;
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
    public void registerJob(@RequestBody List<JobRegisterParam> jobRegisterParams) {
        jobRegisterService.registerJob(jobRegisterParams);
    }

    @GetMapping("/trigger")
    public void triggerJob() {
        jobRegisterService.triggerJob();
    }

}
