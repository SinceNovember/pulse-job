
package com.simple.pulsejob.admin.business.service.impl;

import com.simple.pulsejob.admin.business.service.IJobInstanceService;
import com.simple.pulsejob.admin.common.model.entity.JobInstance;
import com.simple.pulsejob.admin.common.model.enums.JobInstanceStatus;
import com.simple.pulsejob.admin.persistence.mapper.JobInstanceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 任务实例服务实现.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobInstanceServiceImpl implements IJobInstanceService {

    private final JobInstanceMapper jobInstanceMapper;

    @Override
    @Transactional
    public Long createInstance(Integer jobId, Integer executorId) {
        JobInstance instance = new JobInstance();
        instance.setJobId(jobId);
        instance.setExecutorId(executorId);
        instance.setTriggerTime(LocalDateTime.now());
        instance.setStatus(JobInstanceStatus.PENDING.getValue());
        instance.setRetryCount(0);

        JobInstance jobInstance = jobInstanceMapper.save(instance);

        log.debug("Created job instance: instanceId={}, jobId={}, executorId={}",
                instance.getId(), jobId, executorId);

        return jobInstance.getId();
    }

    @Override
    @Transactional
    public void markTransported(Long instanceId) {
        updateStatus(instanceId, JobInstanceStatus.TRANSPORTED);
    }

    @Override
    public void markTransportFailed(Long instanceId) {
        updateStatus(instanceId, JobInstanceStatus.TRANSPORT_FAILED);

    }

    @Override
    @Transactional
    public void markRunning(Long instanceId, LocalDateTime startTime) {
        updateStatus(instanceId, JobInstanceStatus.RUNNING);
    }

    @Override
    @Transactional
    public void markSuccess(Long instanceId, LocalDateTime endTime) {
        updateStatus(instanceId, JobInstanceStatus.SUCCESS);

    }

    @Override
    @Transactional
    public void markFailed(Long instanceId, LocalDateTime endTime, String errorMsg) {
        updateStatus(instanceId, JobInstanceStatus.FAILED);

    }

    @Override
    @Transactional
    public void markTimeout(Long instanceId) {
        updateStatus(instanceId, JobInstanceStatus.TIMEOUT);
    }

    @Override
    public void updateStatus(Long instanceId, JobInstanceStatus status) {
        jobInstanceMapper.updateStatus(instanceId, status.getValue());
    }

    @Override
    public JobInstance getById(Long instanceId) {
        return jobInstanceMapper.findById(instanceId).orElse(null);
    }

    @Override
    @Transactional
    public void incrementRetryCount(Long instanceId) {
        jobInstanceMapper.findById(instanceId).ifPresent(instance -> {
            instance.setRetryCount(instance.getRetryCount() + 1);
            jobInstanceMapper.save(instance);
        });
    }
}

