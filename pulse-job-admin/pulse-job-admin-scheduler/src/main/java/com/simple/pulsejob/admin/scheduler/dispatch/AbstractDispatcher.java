package com.simple.pulsejob.admin.scheduler.dispatch;

import com.simple.plusejob.serialization.Serializer;
import com.simple.plusejob.serialization.SerializerType;
import com.simple.pulsejob.admin.common.model.enums.SerializerTypeEnum;
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

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractDispatcher implements Dispatcher {

    protected final ExecutorChannelGroupManager channelGroupManager;

    protected final SchedulerInterceptorChain schedulerInterceptorChain;

    protected final LoadBalancerFactory loadBalancerFactory;

    protected final JobFilterChains chains;

    protected final SerializerFactory serializerFactory;


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
        // 保存运行时状态到 context（供拦截器和回调使用）
        context.setChannel(channel);
        
        if (isFirstAttempt) {
            // 首次调用：触发 beforeTransport（创建 JobInstance）
            schedulerInterceptorChain.beforeTransport(context);
        }

        JRequest request = createRequest(channel, context);
        context.setRequest(request);

        final DefaultInvokeFuture future = DefaultInvokeFuture
            .with(request.instanceId(), channel, 0, null, type());

        channel.write(request.payload(), new JFutureListener<>() {
            @Override
            public void operationSuccess(JChannel ch) {
                schedulerInterceptorChain.afterTransport(context);
            }

            @Override
            public void operationFailure(JChannel ch, Throwable cause) {
                schedulerInterceptorChain.onTransportFailure(context, cause);
            }
        });
        return future;
    }

    private JRequest createRequest(JChannel channel, ScheduleContext context) {
        Integer jobId = context.getJobId();
        String handlerName = context.getJobHandler();
        String args = context.getJobParams();
        Long instanceId = context.getInstanceId();

        MessageWrapper message = new MessageWrapper(jobId, handlerName, args);
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
