package com.simple.pulsejob.admin.service.impl;

import com.simple.pulsejob.admin.mapping.JobInfoMapping;
import com.simple.pulsejob.admin.model.entity.JobInfo;
import com.simple.pulsejob.admin.mapper.JobInfoMapper;
import com.simple.pulsejob.admin.model.param.JobInfoParam;
import com.simple.pulsejob.admin.service.IJobInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JobInfoServiceImpl implements IJobInfoService {

    private final JobInfoMapper jobInfoMapper;

    @Override
    public void addJobInfo(JobInfoParam jobInfoParam) {
        JobInfo jobInfo = JobInfoMapping.INSTANCE.toJobInfo(jobInfoParam);
        jobInfoMapper.save(jobInfo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<JobInfo> getJobInfoById(Integer id) {
        return jobInfoMapper.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobInfo> getAllJobInfos() {
        return jobInfoMapper.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobInfo> getJobInfosByHandler(String jobHandler) {
        return jobInfoMapper.findByJobHandler(jobHandler);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobInfo> getJobInfosByExecutorId(Integer executorId) {
        return jobInfoMapper.findByExecutorId(executorId);
    }

    @Override
    public JobInfo updateJobInfo(JobInfo jobInfo) {
        if (jobInfo.getId() == null) {
            throw new IllegalArgumentException("更新任务信息时ID不能为空");
        }
        return jobInfoMapper.save(jobInfo);
    }

    @Override
    public void deleteJobInfo(Integer id) {
        jobInfoMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAddJobInfos(List<JobInfoParam> jobInfoParams) {
        if (jobInfoParams == null || jobInfoParams.isEmpty()) {
            throw new IllegalArgumentException("批量保存的任务信息列表不能为空");
        }
        List<JobInfo> jobInfos = JobInfoMapping.INSTANCE.toJobInfoList(jobInfoParams);
        jobInfoMapper.saveAll(jobInfos);
    }
}    