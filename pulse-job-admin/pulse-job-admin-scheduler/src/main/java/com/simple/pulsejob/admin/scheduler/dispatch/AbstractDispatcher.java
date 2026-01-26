package com.simple.pulsejob.admin.scheduler.dispatch;

import com.simple.plusejob.serialization.Serializer;
import com.simple.plusejob.serialization.SerializerType;
import com.simple.pulsejob.admin.common.model.entity.JobInstance;
import com.simple.pulsejob.admin.common.model.enums.JobInstanceStatus;
import com.simple.pulsejob.admin.common.model.enums.SerializerTypeEnum;
import com.simple.pulsejob.admin.persistence.mapper.JobInstanceMapper;
import com.simple.pulsejob.admin.scheduler.JobInstanceStatusManager;
import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import com.simple.pulsejob.admin.scheduler.channel.ExecutorChannelGroupManager;
import com.simple.pulsejob.admin.scheduler.factory.LoadBalancerFactory;
import com.simple.pulsejob.admin.scheduler.factory.SerializerFactory;
import com.simple.pulsejob.admin.scheduler.filter.JobFilterChains;
import com.simple.pulsejob.admin.scheduler.future.DefaultInvokeFuture;
import com.simple.pulsejob.admin.scheduler.interceptor.SchedulerInterceptorChain;
import com.simple.pulsejob.admin.scheduler.load.balance.LoadBalancer;
import com.simple.pulsejob.transport.JProtocolHeader;
import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.channel.JChannelGroup;
import com.simple.pulsejob.transport.channel.JFutureListener;
import com.simple.pulsejob.transport.metadata.ExecutorKey;
import com.simple.pulsejob.transport.metadata.MessageWrapper;
import com.simple.pulsejob.transport.payload.JRequestPayload;
import com.simple.pulsejob.transport.payload.PayloadSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractDispatcher implements Dispatcher {

    protected final ExecutorChannelGroupManager channelGroupManager;
    protected final SchedulerInterceptorChain schedulerInterceptorChain;
    protected final LoadBalancerFactory loadBalancerFactory;
    protected final JobFilterChains chains;
    protected final SerializerFactory serializerFactory;
    protected final JobInstanceMapper jobInstanceMapper;
    protected final JobInstanceStatusManager statusManager;


    protected JChannel select(ScheduleContext context) {
        JChannelGroup channelGroup = channelGroupManager.find(context.getExecutorKey());
        LoadBalancer loadBalancer = loadBalancerFactory.get(context.getLoadBalanceType());
        return loadBalancer.select(channelGroup);
    }

    protected JChannelGroup channelGroup(ExecutorKey executorKey) {
        return channelGroupManager.find(executorKey);
    }

    protected Serializer serializer(SerializerTypeEnum serializerType) {
        return serializerFactory.get(serializerType);
    }


    /**
     * 写入请求（首次调用，触发 beforeTransport 创建 Instance）
     */
    protected DefaultInvokeFuture write(final JChannel channel, ScheduleContext context) {
        return doWrite(channel, context, true);
    }

    /**
     * 重试写入（复用已有 instanceId，跳过 beforeTransport）
     */
    protected DefaultInvokeFuture writeRetry(final JChannel channel, ScheduleContext context) {
        return doWrite(channel, context, false);
    }

    private DefaultInvokeFuture doWrite(final JChannel channel, ScheduleContext context, boolean isFirstAttempt) {
        if (isFirstAttempt) {
            // ✅ 核心流程：创建 JobInstance（固定逻辑，不可跳过）
            Long instanceId = createJobInstance(context);
            context.setInstanceId(instanceId);
            
            log.debug("JobInstance created in core flow: instanceId={}, jobId={}", 
                    instanceId, context.getJobId());
        }

        final JRequest request = createRequest(channel, context);
        final long instanceId = request.instanceId();

        // 计算 Admin 侧超时时间（比客户端多 5 秒，确保客户端有时间返回超时错误）
        int timeoutSeconds = context.getTimeoutSeconds();
        long timeoutMillis = timeoutSeconds > 0 ? (timeoutSeconds + 5) * 1000L : 0;

        final DefaultInvokeFuture future = DefaultInvokeFuture
            .with(instanceId, channel, timeoutMillis, null, type());

        // 拦截器扩展点（可选：记录日志等）
        schedulerInterceptorChain.beforeTransport(context, request);

        channel.write(request.payload(), new JFutureListener<>() {
            @Override
            public void operationSuccess(JChannel ch) {
                // ✅ 核心流程：更新状态为已发送
                statusManager.markTransported(instanceId);
                
                // 拦截器扩展点（可选）
                schedulerInterceptorChain.afterTransport(context, request);
            }

            @Override
            public void operationFailure(JChannel ch, Throwable cause) {
                // ⚠️ 重要：让 Future 失败，触发上层 whenComplete 回调
                future.completeExceptionally(cause);

                // ✅ 核心流程：更新状态为发送失败
                statusManager.markTransportFailed(instanceId);
                
                // 拦截器扩展点（可选）
                schedulerInterceptorChain.onTransportFailure(context, request, cause);
            }
        });
        return future;
    }

    /**
     * 核心流程：创建 JobInstance 记录
     * <p>直接使用 persistence 层，避免 scheduler ↔ business 循环依赖</p>
     *
     * @param context 调度上下文
     * @return 创建的 instanceId
     */
    private Long createJobInstance(ScheduleContext context) {
        JobInstance instance = new JobInstance();
        instance.setJobId(context.getJobId());
        instance.setExecutorId(context.getExecutorId());
        instance.setTriggerTime(LocalDateTime.now());
        instance.setStatus(JobInstanceStatus.PENDING.getValue());
        instance.setRetryCount(0);
        
        JobInstance saved = jobInstanceMapper.save(instance);
        return saved.getId();
    }

    private JRequest createRequest(JChannel channel, ScheduleContext context) {
        Integer jobId = context.getJobId();
        String handlerName = context.getJobHandler();
        String args = context.getJobParams();
        Long instanceId = context.getInstanceId();
        int timeoutSeconds = context.getTimeoutSeconds();

        MessageWrapper message = new MessageWrapper(jobId, handlerName, args, timeoutSeconds);
        // 将 SerializerTypeEnum 转换为 SerializerType
        SerializerTypeEnum serializerTypeEnum = context.getSerializerType();
        SerializerType serializerType = serializerTypeEnum != null 
            ? serializerTypeEnum.toSerializerType() 
            : SerializerType.JAVA;
        
        JRequestPayload payload = PayloadSerializer.createRequest(
            instanceId, channel, serializerType, message, JProtocolHeader.TRIGGER_JOB);

        return new JRequest(payload, message);
    }
}
