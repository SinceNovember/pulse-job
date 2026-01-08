package com.simple.pulsejob.admin.scheduler.dispatch;

import com.simple.plusejob.serialization.SerializerType;
import com.simple.plusejob.serialization.io.OutputBuf;
import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import com.simple.pulsejob.admin.scheduler.channel.ExecutorChannelGroupManager;
import com.simple.pulsejob.admin.scheduler.factory.LoadBalancerFactory;
import com.simple.pulsejob.admin.scheduler.factory.SerializerFactory;
import com.simple.pulsejob.admin.scheduler.filter.JobFilterChains;
import com.simple.pulsejob.admin.scheduler.future.DefaultInvokeFuture;
import com.simple.pulsejob.admin.scheduler.future.InvokeFuture;
import com.simple.pulsejob.admin.scheduler.interceptor.SchedulerInterceptor;
import com.simple.pulsejob.admin.scheduler.interceptor.SchedulerInterceptorChain;
import com.simple.pulsejob.transport.JProtocolHeader;
import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.channel.JChannelGroup;
import com.simple.pulsejob.transport.metadata.MessageWrapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BroadcastDispatcher extends AbstractDispatcher {

    public BroadcastDispatcher(ExecutorChannelGroupManager channelGroupManager,
                               SchedulerInterceptorChain schedulerInterceptorChain,
                               LoadBalancerFactory loadBalancerFactory,
                               JobFilterChains chains, SerializerFactory serializerFactory) {
        super(channelGroupManager, schedulerInterceptorChain, loadBalancerFactory, chains, serializerFactory);
    }

    @Override
    public InvokeFuture dispatch(ScheduleContext context) {
        JChannelGroup channelGroup = channelGroup(context.getExecutorKey());
        List<JChannel> channels = channelGroup.channels();
        DefaultInvokeFuture[] futures = new DefaultInvokeFuture[channels.size()];
        for (int i = 0; i < channels.size(); i++) {
            JChannel channel = channels.get(i);
            futures[i] = write(channel, context);
        }
        return null;
    }

    @Override
    public Type type() {
        return Type.BROADCAST;
    }
}
