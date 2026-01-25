package com.simple.pulsejob.admin.scheduler.dispatch;

import com.simple.pulsejob.admin.common.model.enums.DispatchTypeEnum;
import com.simple.pulsejob.admin.persistence.mapper.JobInstanceMapper;
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
                                LoadBalancerFactory loadBalancerFactory, 
                                JobFilterChains chains,
                                SerializerFactory serializerFactory,
                                JobInstanceMapper jobInstanceMapper) {
        super(channelGroupManager, schedulerInterceptorChain, loadBalancerFactory, chains, 
              serializerFactory, jobInstanceMapper);
    }

    @Override
    public InvokeFuture dispatch(ScheduleContext context) {
        // 通过软负载均衡选择一个channel
        JChannel channel = select(context);
        if (channel == null) {
            throw new IllegalStateException("No available channel for executor: " + context.getExecutorKey());
        }
        return write(channel, context);
    }

    @Override
    public InvokeFuture dispatchRetry(ScheduleContext context) {
        // 重试时重新选择 channel（可能切换到其他节点）
        JChannel channel = select(context);
        if (channel == null) {
            throw new IllegalStateException("No available channel for retry, executor: " + context.getExecutorKey());
        }
        return writeRetry(channel, context);
    }

    @Override
    public DispatchTypeEnum type() {
        return DispatchTypeEnum.ROUND;
    }
}