package com.simple.pulsejob.admin.service;

import java.util.List;
import com.simple.pulsejob.admin.model.param.JobRegisterParam;

public interface IJobRegisterService {

    void registerJob(List<JobRegisterParam> jobRegisterParams);

    void triggerJob();
}
