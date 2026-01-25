
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
        jobInstanceMapper.findById(instanceId).ifPresent(instance -> {
            instance.setStatus(JobInstanceStatus.SUCCESS.getValue());
            instance.setEndTime(endTime);
            jobInstanceMapper.save(instance);
        });
    }

    @Override
    @Transactional
    public void markSuccessWithResult(Long instanceId, LocalDateTime endTime, String result) {
        jobInstanceMapper.findById(instanceId).ifPresent(instance -> {
            instance.setStatus(JobInstanceStatus.SUCCESS.getValue());
            instance.setEndTime(endTime);
            instance.setResult(result);
            jobInstanceMapper.save(instance);
            log.info("任务实例执行成功: instanceId={}, result={}", instanceId, 
                    result != null && result.length() > 100 ? result.substring(0, 100) + "..." : result);
        });
    }

    @Override
    @Transactional
    public void markFailed(Long instanceId, LocalDateTime endTime, String errorMsg) {
        jobInstanceMapper.findById(instanceId).ifPresent(instance -> {
            instance.setStatus(JobInstanceStatus.FAILED.getValue());
            instance.setEndTime(endTime);
            instance.setErrorMessage(truncateErrorMsg(errorMsg));
            jobInstanceMapper.save(instance);
        });
    }

    @Override
    @Transactional
    public void markFailedWithDetail(Long instanceId, LocalDateTime endTime, String errorMsg, String errorDetail) {
        jobInstanceMapper.findById(instanceId).ifPresent(instance -> {
            instance.setStatus(JobInstanceStatus.FAILED.getValue());
            instance.setEndTime(endTime);
            // 拼接错误信息和详情
            String fullError = errorMsg;
            if (errorDetail != null && !errorDetail.isEmpty()) {
                fullError = errorMsg + "\n\n" + errorDetail;
            }
            instance.setErrorMessage(truncateErrorMsg(fullError));
            jobInstanceMapper.save(instance);
            log.error("任务实例执行失败: instanceId={}, error={}", instanceId, errorMsg);
        });
    }

    @Override
    @Transactional
    public void updateExecutorAddress(Long instanceId, String executorAddress) {
        jobInstanceMapper.findById(instanceId).ifPresent(instance -> {
            instance.setExecutorAddress(executorAddress);
            jobInstanceMapper.save(instance);
        });
    }

    /**
     * 截断错误信息，避免超过数据库字段长度
     */
    private String truncateErrorMsg(String errorMsg) {
        if (errorMsg == null) {
            return null;
        }
        return errorMsg.length() > 2000 ? errorMsg.substring(0, 2000) : errorMsg;
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

