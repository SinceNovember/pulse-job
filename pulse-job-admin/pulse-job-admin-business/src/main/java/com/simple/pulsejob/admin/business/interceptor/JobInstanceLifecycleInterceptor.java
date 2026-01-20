package com.simple.pulsejob.admin.business.interceptor;

import com.simple.pulsejob.admin.business.service.IJobInfoService;
import com.simple.pulsejob.admin.business.service.IJobInstanceService;
import com.simple.pulsejob.admin.common.model.dto.JobInfoWithExecutorDTO;
import com.simple.pulsejob.admin.common.model.entity.JobInfo;
import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import com.simple.pulsejob.admin.scheduler.interceptor.SchedulerInterceptor;
import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.JResponse;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.metadata.ExecutorKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobInstanceLifecycleInterceptor implements SchedulerInterceptor {

    private final IJobInstanceService jobInstanceService;
    private final IJobInfoService jobInfoService;

    @Override
    public void beforeSchedule(ScheduleContext context) {
        Integer jobId = context.getJobId();

        JobInfoWithExecutorDTO dto = jobInfoService.getJobInfoWithExecutorNameById(jobId)
                .orElseThrow(() -> new IllegalStateException("JobInfo not found, jobId=" + jobId));

        JobInfo jobInfo = dto.getJobInfo();

        context.setExecutorId(jobInfo.getExecutorId());
        context.setExecutorKey(ExecutorKey.of(dto.getExecutorName()));
        context.setDispatchType(jobInfo.getDispatchType());
        context.setScheduleType(jobInfo.getScheduleType());
        context.setLoadBalanceType(jobInfo.getLoadBalanceType());
        context.setSerializerType(jobInfo.getSerializerType());
        context.setJobHandler(jobInfo.getJobHandler());
        context.setJobParams(jobInfo.getJobParams());
        log.debug("Schedule prepared: jobId={}, executorId={}, executorName={}",
                jobId, jobInfo.getExecutorId(), dto.getExecutorName());
    }

    @Override
    public void beforeTransport(ScheduleContext context, JChannel channel) {
        Integer jobId = context.getJobId();
        Integer executorId = context.getExecutorId();

        Long instanceId = jobInstanceService.createInstance(jobId, executorId);
        context.setInstanceId(instanceId);

        log.info("Job instance created: instanceId={}, jobId={}, executorId={}",
                instanceId, jobId, executorId);
    }

    @Override
    public void afterTransport(ScheduleContext context, JChannel channel, JRequest request) {
        Long instanceId = context.getInstanceId();
        if (instanceId == null) {
            log.warn("Skip markTransported: instanceId is null");
            return;
        }
        jobInstanceService.markTransported(instanceId);
    }

    @Override
    public void onTransportFailure(
            ScheduleContext context,
            JChannel channel,
            JRequest request,
            Throwable throwable) {

        Long instanceId = context.getInstanceId();
        if (instanceId == null) {
            log.warn("Transport failed before instance creation", throwable);
            return;
        }

        jobInstanceService.markTransportFailed(instanceId);
        log.error("Transport failed: instanceId={}", instanceId, throwable);
    }

    @Override
    public void afterSchedule(ScheduleContext context, JChannel channel, JResponse response) {
        // 当前拦截器只关心“调度前 + 传输阶段”
        // 执行结果由其他拦截器处理
    }

    @Override
    public void onScheduleFailure(ScheduleContext context, JChannel channel, JRequest request) {
        // 同上，刻意留空，语义上不处理
    }
}
