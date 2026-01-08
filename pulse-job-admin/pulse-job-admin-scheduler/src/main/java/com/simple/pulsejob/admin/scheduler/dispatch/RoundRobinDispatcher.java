package com.simple.pulsejob.admin.scheduler.dispatch;

import java.util.List;

import com.simple.plusejob.serialization.SerializerType;
import com.simple.plusejob.serialization.io.OutputBuf;
import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import com.simple.pulsejob.admin.scheduler.channel.ExecutorChannelGroupManager;
import com.simple.pulsejob.admin.scheduler.factory.LoadBalancerFactory;
import com.simple.pulsejob.admin.scheduler.factory.SerializerFactory;
import com.simple.pulsejob.admin.scheduler.filter.JobFilterChains;
import com.simple.pulsejob.admin.scheduler.future.InvokeFuture;
import com.simple.pulsejob.admin.scheduler.interceptor.SchedulerInterceptor;
import com.simple.pulsejob.admin.scheduler.interceptor.SchedulerInterceptorChain;
import com.simple.pulsejob.transport.JProtocolHeader;
import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.metadata.MessageWrapper;
import org.springframework.stereotype.Component;

@Component
public class RoundRobinDispatcher extends AbstractDispatcher {

    public RoundRobinDispatcher(ExecutorChannelGroupManager channelGroupManager,
                                SchedulerInterceptorChain schedulerInterceptorChain,
                                LoadBalancerFactory loadBalancerFactory, JobFilterChains chains,
                                SerializerFactory serializerFactory) {
        super(channelGroupManager, schedulerInterceptorChain, loadBalancerFactory, chains, serializerFactory);
    }

    @Override
    public InvokeFuture dispatch(ScheduleContext context) {
        // 通过软负载均衡选择一个channel
        JChannel channel = select(context);
        return write(channel, context);
    }

    @Override
    public Type type() {
        return Type.ROUND;
    }
}