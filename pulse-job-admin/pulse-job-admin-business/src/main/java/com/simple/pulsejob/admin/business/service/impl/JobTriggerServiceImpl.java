package com.simple.pulsejob.admin.business.service.impl;

import com.simple.pulsejob.admin.business.service.IJobTriggerService;
import com.simple.pulsejob.admin.common.model.dto.JobInfoWithExecutorDTO;
import com.simple.pulsejob.admin.common.model.entity.JobInfo;
import com.simple.pulsejob.admin.persistence.mapper.JobInfoMapper;
import com.simple.pulsejob.admin.scheduler.JobScheduleEngine;
import com.simple.pulsejob.admin.scheduler.ScheduleConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
        JobInfoWithExecutorDTO jobInfoWithExecutorDTO = jobInfoMapper.findWithExecutorNameById(jobId)
                .orElseThrow(() -> new IllegalStateException("JobInfo not found, jobId=" + jobId));

        JobInfo jobInfo = jobInfoWithExecutorDTO.getJobInfo();

        // 检查任务是否启用
        if (!jobInfo.isEnabled()) {
            log.warn("任务已禁用，无法触发: jobId={}", jobId);
            throw new IllegalStateException("任务已禁用");
        }

        log.info("手动触发任务开始: jobId={}, handler={}", jobId, jobInfo.getJobHandler());

        try {
            // 构建调度配置
            ScheduleConfig config = buildScheduleConfig(jobInfo, params);
            jobScheduleEngine.schedule(config);
        } catch (Exception e) {
            log.error("任务触发失败: jobId={}", jobId, e);
            throw new RuntimeException("任务触发失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从 JobInfo 构建 ScheduleConfig
     */
    private ScheduleConfig buildScheduleConfig(JobInfo jobInfo, String overrideParams) {
        ScheduleConfig config = new ScheduleConfig();

        // 任务基本信息
        config.setJobId(jobInfo.getId());
        config.setJobHandler(jobInfo.getJobHandler());

        // 参数：如果传入了 overrideParams 则使用传入的，否则使用任务配置的
        config.setJobParams(StringUtils.hasText(overrideParams) ? overrideParams : jobInfo.getJobParams());

        // 调度配置
        config.setScheduleType(jobInfo.getScheduleType());
        config.setScheduleExpression(jobInfo.getScheduleRate());
        config.setDispatchType(jobInfo.getDispatchType());
        config.setLoadBalanceType(jobInfo.getLoadBalanceType());
        config.setSerializerType(jobInfo.getSerializerType());

        // 重试和同步配置
        config.setRetries(jobInfo.getMaxRetryTimes() != null ? jobInfo.getMaxRetryTimes() : 1);
        config.setSync(false); // 手动触发默认异步

        return config;
    }
}
