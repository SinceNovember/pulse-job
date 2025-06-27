package com.simple.pulsejob.admin.service;

import java.util.List;
import com.simple.pulsejob.admin.model.param.JobInfoParam;

public interface IJobRegisterService {

    void registerJob(List<JobInfoParam> jobRegisterParams);

    void triggerJob();
}
