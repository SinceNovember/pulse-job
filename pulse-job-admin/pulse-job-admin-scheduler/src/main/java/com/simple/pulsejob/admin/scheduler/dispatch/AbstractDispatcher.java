package com.simple.pulsejob.admin.scheduler.dispatch;

import java.util.List;

import com.simple.plusejob.serialization.Serializer;
import com.simple.pulsejob.admin.scheduler.channel.ExecutorChannelGroupManager;
import com.simple.pulsejob.admin.scheduler.filter.JobFilterChains;
import com.simple.pulsejob.admin.scheduler.future.DefaultInvokeFuture;
import com.simple.pulsejob.admin.scheduler.interceptor.JobInterceptor;
import com.simple.pulsejob.admin.scheduler.load.balance.LoadBalancer;
import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.channel.JChannelGroup;
import com.simple.pulsejob.transport.channel.JFutureListener;
import com.simple.pulsejob.transport.metadata.ExecutorKey;
import com.simple.pulsejob.transport.payload.JRequestPayload;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

@Slf4j
@SuperBuilder
public abstract class AbstractDispatcher implements Dispatcher {

    private final ExecutorChannelGroupManager channelGroupManager;

    private final List<JobInterceptor> interceptors;

    private final LoadBalancer loadBalancer;

    private final Serializer serializerImpl;                    // 序列化/反序列化impl

    private final JobFilterChains chains;

    protected JChannel select(ExecutorKey executorKey) {
        JChannelGroup channelGroup = channelGroupManager.find(executorKey);
        return loadBalancer.select(channelGroup);
    }

    protected JChannelGroup channelGroup(ExecutorKey executorKey) {
        return channelGroupManager.find(executorKey);
    }

    protected Serializer serializer() {
        return serializerImpl;
    }

    protected DefaultInvokeFuture write(final JChannel channel, final JRequest request, final DispatchType dispatchType) {
        final JRequestPayload payload = request.payload();

        final DefaultInvokeFuture future = DefaultInvokeFuture
            .with(request.invokeId(), channel, 0, null, dispatchType)
            .interceptors(interceptors);

        if (!CollectionUtils.isEmpty(interceptors)) {
            for (int i = 0; i < interceptors.size(); i++) {
                interceptors.get(i).beforeInvoke(request, channel);
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
