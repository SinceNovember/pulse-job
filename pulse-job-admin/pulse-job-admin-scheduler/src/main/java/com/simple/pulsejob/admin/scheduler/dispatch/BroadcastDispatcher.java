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
import com.simple.pulsejob.admin.scheduler.interceptor.TransportInterceptor;
import com.simple.pulsejob.transport.JProtocolHeader;
import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.channel.JChannelGroup;
import com.simple.pulsejob.transport.metadata.MessageWrapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BroadcastDispatcher extends AbstractDispatcher {

    public BroadcastDispatcher(ExecutorChannelGroupManager channelGroupManager, List<TransportInterceptor> interceptors,
                               LoadBalancerFactory loadBalancerFactory,
                               JobFilterChains chains, SerializerFactory serializerFactory) {
        super(channelGroupManager, interceptors, loadBalancerFactory, chains, serializerFactory);
    }

    @Override
    public InvokeFuture dispatch(JRequest request, ScheduleContext context) {
        final MessageWrapper message = request.getMessage();
        JChannelGroup channelGroup = channelGroup(context.getExecutorKey());
        List<JChannel> channels = channelGroup.channels();
        SerializerType serializerType = context.getSerializerType();
        DefaultInvokeFuture[] futures = new DefaultInvokeFuture[channels.size()];

        for (int i = 0; i < channels.size(); i++) {
            JChannel channel = channels.get(i);
            OutputBuf outputBuf =
                serializer(serializerType).writeObject(channel.allocOutputBuf(), message);
            request.outputBuf(serializerType, JProtocolHeader.TRIGGER_JOB, outputBuf);
            futures[i] = write(channel, request, type());
        }
        return null;
    }

    @Override
    public Type type() {
        return Type.BROADCAST;
    }
}
