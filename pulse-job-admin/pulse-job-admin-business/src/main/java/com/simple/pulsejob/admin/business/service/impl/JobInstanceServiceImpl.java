
package com.simple.pulsejob.admin.business.service.impl;

import com.simple.pulsejob.admin.common.model.entity.JobInstance;
import com.simple.pulsejob.admin.common.model.enums.JobInstanceStatus;
import com.simple.pulsejob.admin.persistence.mapper.JobInstanceMapper;
import com.simple.pulsejob.admin.scheduler.instance.JobInstanceManager;
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
public class JobInstanceServiceImpl implements JobInstanceManager {

    private final JobInstanceMapper jobInstanceMapper;

    @Override
    @Transactional
    public JobInstance createInstance(Long jobId, Long executorId) {
        JobInstance instance = new JobInstance();
        instance.setJobId(12L);
        instance.setExecutorId(12L);
        instance.setTriggerTime(LocalDateTime.now());
        instance.setStatus(JobInstanceStatus.PENDING.getValue());
        instance.setRetryCount(0);

        jobInstanceMapper.save(instance);

        log.debug("Created job instance: id={}, jobId={}, executorId={}",
                instance.getId(), jobId, executorId);

        return instance;
    }

    @Override
    @Transactional
    public void markDispatched(Long instanceId) {
        updateStatus(instanceId, JobInstanceStatus.DISPATCHED);
    }

    @Override
    @Transactional
    public void markRunning(Long instanceId, LocalDateTime startTime) {
        jobInstanceMapper.findById(instanceId).ifPresent(instance -> {
            instance.setStatus(JobInstanceStatus.RUNNING.getValue());
            instance.setStartTime(startTime);
            jobInstanceMapper.save(instance);
        });
    }

    @Override
    @Transactional
    public void markSuccess(Long instanceId, LocalDateTime endTime) {
        jobInstanceMapper.findById(instanceId).ifPresent(instance -> {
            instance.setStatus(JobInstanceStatus.SUCCESS.getValue());
            instance.setEndTime(endTime);
            jobInstanceMapper.save(instance);
        });
    }

    @Override
    @Transactional
    public void markFailed(Long instanceId, LocalDateTime endTime, String errorMsg) {
        jobInstanceMapper.findById(instanceId).ifPresent(instance -> {
            instance.setStatus(JobInstanceStatus.FAILED.getValue());
            instance.setEndTime(endTime);
            // 如果需要存储错误信息，可以在 JobInstance 中添加 errorMsg 字段
            jobInstanceMapper.save(instance);
        });
    }

    @Override
    @Transactional
    public void markTimeout(Long instanceId) {
        jobInstanceMapper.findById(instanceId).ifPresent(instance -> {
            instance.setStatus(JobInstanceStatus.TIMEOUT.getValue());
            instance.setEndTime(LocalDateTime.now());
            jobInstanceMapper.save(instance);
        });
    }

    @Override
    @Transactional
    public void updateStatus(Long instanceId, JobInstanceStatus status) {
        jobInstanceMapper.findById(instanceId).ifPresent(instance -> {
            instance.setStatus(status.getValue());
            jobInstanceMapper.save(instance);
        });
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

