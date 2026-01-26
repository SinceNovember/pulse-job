package com.simple.pulsejob.admin.business.interceptor;

import com.simple.pulsejob.admin.business.service.IJobInfoService;
import com.simple.pulsejob.admin.business.service.IJobInstanceService;
import com.simple.pulsejob.admin.common.model.dto.JobInfoWithExecutorDTO;
import com.simple.pulsejob.admin.common.model.entity.JobInfo;
import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import com.simple.pulsejob.admin.scheduler.interceptor.SchedulerInterceptor;
import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.metadata.ExecutorKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 任务实例生命周期拦截器.
 *
 * <p>职责：</p>
 * <ul>
 *   <li>beforeSchedule - 查询 JobInfo，填充调度上下文（仅调用一次）</li>
 *   <li>beforeTransport - 记录日志（广播/分片模式下会调用多次）</li>
 *   <li>afterTransport - 扩展处理（广播/分片模式下会调用多次）</li>
 *   <li>onTransportFailure - 错误处理（广播/分片模式下会调用多次）</li>
 * </ul>
 *
 * <p>注意：</p>
 * <ul>
 *   <li>instanceId 创建和状态更新已移至 AbstractDispatcher 核心流程</li>
 *   <li>广播/分片模式下通过 request.instanceId() 区分不同实例</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobInstanceLifecycleInterceptor implements SchedulerInterceptor {

    private final IJobInstanceService jobInstanceService;
    private final IJobInfoService jobInfoService;

    @Override
    public void beforeSchedule(ScheduleContext context) {

    }

    @Override
    public void beforeTransport(ScheduleContext context, JRequest request) {
        // ✅ instanceId 已由核心流程创建
        // 这里只做扩展：记录日志、设置触发类型等
        log.info("beforeTransport: instanceId={}, jobId={}, executorId={}", 
                request.instanceId(), context.getJobId(), context.getExecutorId());
    }

    @Override
    public void afterTransport(ScheduleContext context, JRequest request) {
        // ✅ 状态更新已移至核心流程
        // 这里只做扩展：记录日志、通知等
        log.debug("afterTransport callback: instanceId={}", request.instanceId());
    }

    @Override
    public void onTransportFailure(ScheduleContext context, JRequest request, Throwable throwable) {
        // ✅ 状态更新已移至核心流程
        // 这里只做扩展：记录错误日志、告警通知等
        log.error("Transport failed callback: instanceId={}, error={}", 
                request.instanceId(), throwable.getMessage(), throwable);
    }

    // afterSchedule 和 onScheduleFailure 使用默认空实现
    // 执行结果由 JobInstanceResultInterceptor 处理
}
