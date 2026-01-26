package com.simple.pulsejob.admin.scheduler.dispatch;

import com.simple.pulsejob.admin.common.model.enums.DispatchTypeEnum;
import com.simple.pulsejob.admin.persistence.mapper.JobInstanceMapper;
import com.simple.pulsejob.admin.scheduler.JobInstanceStatusManager;
import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import com.simple.pulsejob.admin.scheduler.channel.ExecutorChannelGroupManager;
import com.simple.pulsejob.admin.scheduler.factory.LoadBalancerFactory;
import com.simple.pulsejob.admin.scheduler.factory.SerializerFactory;
import com.simple.pulsejob.admin.scheduler.filter.JobFilterChains;
import com.simple.pulsejob.admin.scheduler.future.DefaultInvokeFuture;
import com.simple.pulsejob.admin.scheduler.future.InvokeFuture;
import com.simple.pulsejob.admin.scheduler.interceptor.SchedulerInterceptorChain;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.channel.JChannelGroup;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BroadcastDispatcher extends AbstractDispatcher {

    public BroadcastDispatcher(ExecutorChannelGroupManager channelGroupManager,
                               SchedulerInterceptorChain schedulerInterceptorChain,
                               LoadBalancerFactory loadBalancerFactory,
                               JobFilterChains chains, 
                               SerializerFactory serializerFactory,
                               JobInstanceMapper jobInstanceMapper,
                               JobInstanceStatusManager statusManager) {
        super(channelGroupManager, schedulerInterceptorChain, loadBalancerFactory, chains, 
              serializerFactory, jobInstanceMapper, statusManager);
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
    public InvokeFuture dispatchRetry(ScheduleContext context) {
        // 广播模式不支持重试（Failover 策略已在上层阻止）
        throw new UnsupportedOperationException("Broadcast dispatch does not support retry");
    }

    @Override
    public DispatchTypeEnum type() {
        return DispatchTypeEnum.BROADCAST;
    }
}
