package com.simple.pulsejob.admin.scheduler.dispatch;

import com.simple.plusejob.serialization.Serializer;
import com.simple.plusejob.serialization.SerializerType;
import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import com.simple.pulsejob.admin.scheduler.channel.ExecutorChannelGroupManager;
import com.simple.pulsejob.admin.scheduler.factory.LoadBalancerFactory;
import com.simple.pulsejob.admin.scheduler.factory.SerializerFactory;
import com.simple.pulsejob.admin.scheduler.filter.JobFilterChains;
import com.simple.pulsejob.admin.scheduler.future.DefaultInvokeFuture;
import com.simple.pulsejob.admin.scheduler.interceptor.SchedulerInterceptor;
import com.simple.pulsejob.admin.scheduler.load.balance.LoadBalancer;
import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.channel.JChannelGroup;
import com.simple.pulsejob.transport.channel.JFutureListener;
import com.simple.pulsejob.transport.metadata.ExecutorKey;
import com.simple.pulsejob.transport.payload.JRequestPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractDispatcher implements Dispatcher {

    protected final ExecutorChannelGroupManager channelGroupManager;

    protected final List<SchedulerInterceptor> interceptors;

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

    protected Serializer serializer(SerializerType serializerType) {
        return serializerFactory.get(serializerType);
    }


    protected DefaultInvokeFuture write(final JChannel channel, final JRequest request, final Type dispatchType) {
        final JRequestPayload payload = request.payload();

        final DefaultInvokeFuture future = DefaultInvokeFuture
            .with(request.instanceId(), channel, 0, null, dispatchType)
            .interceptors(interceptors);

        if (!CollectionUtils.isEmpty(interceptors)) {
            for (SchedulerInterceptor interceptor : interceptors) {
                interceptor.beforeInvoke(request, channel);
            }
        }

        channel.write(payload, new JFutureListener<>() {
            @Override
            public void operationSuccess(JChannel channel) throws Exception {
            }

            @Override
            public void operationFailure(JChannel channel, Throwable cause) throws Exception {
            }
        });
        return future;
    }
}
