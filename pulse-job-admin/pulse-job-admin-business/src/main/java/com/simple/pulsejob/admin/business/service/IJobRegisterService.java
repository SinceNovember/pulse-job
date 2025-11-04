package com.simple.pulsejob.admin.business.service;


import java.util.List;
import com.simple.pulsejob.admin.common.model.param.JobInfoParam;

public interface IJobRegisterService {

    void registerJob(List<JobInfoParam> jobRegisterParams);

    void triggerJob();
}
