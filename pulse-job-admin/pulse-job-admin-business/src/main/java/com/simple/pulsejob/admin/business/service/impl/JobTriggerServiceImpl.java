package com.simple.pulsejob.admin.business.service.impl;

import com.simple.pulsejob.admin.business.service.IJobTriggerService;
import com.simple.pulsejob.admin.common.model.dto.JobInfoWithExecutorDTO;
import com.simple.pulsejob.admin.common.model.entity.JobInfo;
import com.simple.pulsejob.admin.persistence.mapper.JobInfoMapper;
import com.simple.pulsejob.admin.scheduler.JobScheduleEngine;
import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 任务触发服务实现.
 *
 * <p>专门处理任务触发逻辑，与 JobInfoService 分离以避免循环依赖</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobTriggerServiceImpl implements IJobTriggerService {

    private final JobInfoMapper jobInfoMapper;
    private final JobScheduleEngine jobScheduleEngine;

    @Override
    public void trigger(Integer jobId) {
        trigger(jobId, null);
    }

    @Override
    public void trigger(Integer jobId, String params) {
        JobInfoWithExecutorDTO jobDetail = jobInfoMapper.findWithExecutorNameById(jobId)
                .orElseThrow(() -> new IllegalStateException("JobInfo not found, jobId=" + jobId));

        // 检查任务是否启用
        if (!jobDetail.getJobInfo().isEnabled()) {
            log.warn("任务已禁用，无法触发: jobId={}", jobId);
            throw new IllegalStateException("任务已禁用");
        }

        log.info("手动触发任务开始: jobId={}, handler={}, executor={}",
                jobId, jobDetail.getJobInfo().getJobHandler(), jobDetail.getExecutorName());

        try {
            // 构建调度上下文（包含 ExecutorKey）
            ScheduleContext context = ScheduleContext.of(jobDetail, params);
            jobScheduleEngine.schedule(context);
        } catch (Exception e) {
            log.error("任务触发失败: jobId={}", jobId, e);
            throw new RuntimeException("任务触发失败: " + e.getMessage(), e);
        }
    }
}
