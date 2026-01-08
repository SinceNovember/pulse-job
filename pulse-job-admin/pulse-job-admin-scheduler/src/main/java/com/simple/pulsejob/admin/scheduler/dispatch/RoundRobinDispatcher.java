package com.simple.pulsejob.admin.scheduler.dispatch;

import com.simple.pulsejob.admin.common.model.enums.DispatchTypeEnum;
import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import com.simple.pulsejob.admin.scheduler.channel.ExecutorChannelGroupManager;
import com.simple.pulsejob.admin.scheduler.factory.LoadBalancerFactory;
import com.simple.pulsejob.admin.scheduler.factory.SerializerFactory;
import com.simple.pulsejob.admin.scheduler.filter.JobFilterChains;
import com.simple.pulsejob.admin.scheduler.future.InvokeFuture;
import com.simple.pulsejob.admin.scheduler.interceptor.SchedulerInterceptorChain;
import com.simple.pulsejob.transport.channel.JChannel;
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
    public DispatchTypeEnum type() {
        return DispatchTypeEnum.ROUND;
    }
}