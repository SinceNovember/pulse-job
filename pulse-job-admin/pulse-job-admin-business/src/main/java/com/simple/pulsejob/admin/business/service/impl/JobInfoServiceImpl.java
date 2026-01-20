package com.simple.pulsejob.admin.business.service.impl;

import com.simple.pulsejob.admin.business.service.IJobInfoService;
import com.simple.pulsejob.admin.common.mapping.JobInfoMapping;
import com.simple.pulsejob.admin.common.model.dto.JobInfoWithExecutorDTO;
import com.simple.pulsejob.admin.common.model.entity.JobInfo;
import com.simple.pulsejob.admin.common.model.param.JobInfoParam;
import com.simple.pulsejob.admin.persistence.mapper.JobInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobInfoServiceImpl implements IJobInfoService {

    private final JobInfoMapper jobInfoMapper;

    @Override
    public void addJobInfo(JobInfoParam jobInfoParam) {
        JobInfo jobInfo = JobInfoMapping.INSTANCE.toJobInfo(jobInfoParam);
        jobInfo.setCreateTime(LocalDateTime.now());
        jobInfo.setUpdateTime(LocalDateTime.now());
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
        jobInfo.setUpdateTime(LocalDateTime.now());
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
        jobInfos.forEach(jobInfo -> {
            jobInfo.setCreateTime(LocalDateTime.now());
            jobInfo.setUpdateTime(LocalDateTime.now());
        });
        jobInfoMapper.saveAll(jobInfos);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobInfo> findJobsToExecute(LocalDateTime startTime, LocalDateTime endTime) {
        return jobInfoMapper.findJobsToExecute(startTime, endTime, 1);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobInfo> findEnabledJobs() {
        return jobInfoMapper.findByStatus(1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNextExecuteTime(Integer jobId, LocalDateTime nextExecuteTime) {
        jobInfoMapper.updateNextExecuteTime(jobId, nextExecuteTime, LocalDateTime.now());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateExecutionStatus(Integer jobId, LocalDateTime lastExecuteTime, Integer retryTimes) {
        jobInfoMapper.updateExecutionStatus(jobId, lastExecuteTime, retryTimes, LocalDateTime.now());
    }

    @Override
    public Optional<JobInfoWithExecutorDTO> getJobInfoWithExecutorNameById(Integer jobId) {
        return jobInfoMapper.findWithExecutorNameById(jobId);
    }
}    