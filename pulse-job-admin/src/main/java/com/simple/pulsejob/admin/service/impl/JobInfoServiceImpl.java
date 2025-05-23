package com.simple.pulsejob.admin.service.impl;

import com.simple.pulsejob.admin.model.entity.JobInfo;
import com.simple.pulsejob.admin.mapper.JobInfoMapper;
import com.simple.pulsejob.admin.service.IJobInfoService;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JobInfoServiceImpl implements IJobInfoService {

    private final JobInfoMapper jobInfoMapper;

    @Override
    public JobInfo saveJobInfo(JobInfo jobInfo) {
        return jobInfoMapper.save(jobInfo);
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
    public List<JobInfo> getJobInfosByCronType(Short cronType) {
        return jobInfoMapper.findByCronType(cronType);
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
    @Transactional
    public List<JobInfo> batchSaveJobInfos(List<JobInfo> jobInfos) {
        if (jobInfos == null || jobInfos.isEmpty()) {
            throw new IllegalArgumentException("批量保存的任务信息列表不能为空");
        }
        return jobInfoMapper.saveAll(jobInfos);
    }
}    