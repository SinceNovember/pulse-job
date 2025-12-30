package com.simple.pulsejob.admin.business.service.impl;

import com.simple.pulsejob.admin.business.service.IJobLogService;
import com.simple.pulsejob.admin.common.model.entity.JobLog;
import com.simple.pulsejob.admin.common.model.enums.LogLevelEnum;
import com.simple.pulsejob.admin.persistence.mapper.JobLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务日志服务实现类.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobLogServiceImpl implements IJobLogService {

    private final JobLogMapper jobLogMapper;

    // ==================== 存储相关 ====================

    @Override
    @Transactional
    public JobLog save(JobLog jobLog) {
        return jobLogMapper.save(jobLog);
    }

    @Override
    @Transactional
    public List<JobLog> saveBatch(List<JobLog> jobLogs) {
        if (jobLogs == null || jobLogs.isEmpty()) {
            return List.of();
        }
        log.debug("批量保存日志，数量: {}", jobLogs.size());
        return jobLogMapper.saveAll(jobLogs);
    }

    // ==================== 查询相关 ====================

    @Override
    public List<JobLog> findByInvokeId(Long invokeId) {
        return jobLogMapper.findByInvokeIdOrderBySequenceAsc(invokeId);
    }

    @Override
    public Page<JobLog> findByInvokeId(Long invokeId, Pageable pageable) {
        return jobLogMapper.findByInvokeId(invokeId, pageable);
    }

    @Override
    public List<JobLog> findByJobId(Integer jobId) {
        return jobLogMapper.findByJobIdOrderByCreateTimeDesc(jobId);
    }

    @Override
    public Page<JobLog> findByJobId(Integer jobId, Pageable pageable) {
        return jobLogMapper.findByJobId(jobId, pageable);
    }

    @Override
    public Page<JobLog> findByExecutorName(String executorName, Pageable pageable) {
        return jobLogMapper.findByExecutorName(executorName, pageable);
    }

    @Override
    public Page<JobLog> findByLogLevel(LogLevelEnum logLevel, Pageable pageable) {
        return jobLogMapper.findByLogLevel(logLevel, pageable);
    }

    @Override
    public Page<JobLog> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        return jobLogMapper.findByCreateTimeBetween(startTime, endTime, pageable);
    }

    @Override
    public List<JobLog> findErrorLogs(Long invokeId) {
        return jobLogMapper.findErrorLogsByInvokeId(invokeId);
    }

    @Override
    public Page<JobLog> search(Long invokeId, Integer jobId, String executorName,
                               LogLevelEnum logLevel, LocalDateTime startTime,
                               LocalDateTime endTime, Pageable pageable) {
        return jobLogMapper.findByConditions(invokeId, jobId, executorName, logLevel, startTime, endTime, pageable);
    }

    // ==================== 统计相关 ====================

    @Override
    public long countByInvokeId(Long invokeId) {
        return jobLogMapper.countByInvokeId(invokeId);
    }

    @Override
    public long countByJobId(Integer jobId) {
        return jobLogMapper.countByJobId(jobId);
    }

    @Override
    public int getMaxSequence(Long invokeId) {
        Integer maxSeq = jobLogMapper.findMaxSequenceByInvokeId(invokeId);
        return maxSeq != null ? maxSeq : 0;
    }

    // ==================== 删除/清理相关 ====================

    @Override
    @Transactional
    public int deleteByInvokeId(Long invokeId) {
        return jobLogMapper.deleteByInvokeId(invokeId);
    }

    @Override
    @Transactional
    public int deleteByJobId(Integer jobId) {
        return jobLogMapper.deleteByJobId(jobId);
    }

    @Override
    @Transactional
    public int cleanExpiredLogs(int retentionDays) {
        if (retentionDays <= 0) {
            log.warn("日志保留天数无效: {}", retentionDays);
            return 0;
        }
        LocalDateTime beforeTime = LocalDateTime.now().minusDays(retentionDays);
        return cleanLogsBefore(beforeTime);
    }

    @Override
    @Transactional
    public int cleanLogsBefore(LocalDateTime beforeTime) {
        log.info("清理 {} 之前的日志", beforeTime);
        int count = jobLogMapper.deleteByCreateTimeBefore(beforeTime);
        log.info("清理日志完成，删除 {} 条记录", count);
        return count;
    }
}

