package com.simple.pulsejob.admin.scheduler.dispatch;

import java.util.List;

import com.simple.plusejob.serialization.Serializer;
import com.simple.pulsejob.admin.scheduler.channel.ExecutorChannelGroupManager;
import com.simple.pulsejob.admin.scheduler.filter.DefaultScheduleFilterChains;
import com.simple.pulsejob.admin.scheduler.filter.ScheduleContext;
import com.simple.pulsejob.admin.scheduler.interceptor.ScheduleInterceptor;
import com.simple.pulsejob.admin.scheduler.load.balance.LoadBalancer;
import com.simple.pulsejob.common.util.StackTraceUtil;
import com.simple.pulsejob.common.util.SystemClock;
import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.JResponse;
import com.simple.pulsejob.transport.Status;
import com.simple.pulsejob.transport.channel.CopyOnWriteGroupList;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.channel.JChannelGroup;
import com.simple.pulsejob.transport.channel.JFutureListener;
import com.simple.pulsejob.transport.metadata.ExecutorKey;
import com.simple.pulsejob.transport.metadata.MessageWrapper;
import com.simple.pulsejob.transport.metadata.ResultWrapper;
import com.simple.pulsejob.transport.payload.JRequestPayload;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuperBuilder
public abstract class AbstractDispatcher implements Dispatcher {

    private final ExecutorChannelGroupManager channelGroupManager;

    private final List<ScheduleInterceptor> interceptors;

    private final LoadBalancer loadBalancer;

    private final Serializer serializerImpl;                    // 序列化/反序列化impl

    private final DefaultScheduleFilterChains chains;

    protected JChannel select(ExecutorKey executorKey) {
        CopyOnWriteGroupList groups = channelGroupManager.find(executorKey);
        JChannelGroup group = loadBalancer.select(groups, executorKey);
        if (groups != null) {
            if (group.isAvailable()) {
                return group.next();
            }

            long deadline = group.deadlineMillis();
            if (deadline > 0 && SystemClock.millisClock().now() > deadline) {
                boolean removed = groups.remove(group);
                if (removed) {
                    if (log.isWarnEnabled()) {
                        log.warn("Removed channel group: {} in executor: {} on [select].",
                            group, executorKey.exeuctorKeyString());
                    }
                }
            }
        }
        throw new IllegalStateException("No connections");
    }

    protected JChannelGroup[] groups(ExecutorKey executorKey) {
        return channelGroupManager.find(executorKey).getSnapshot();
    }



    protected Serializer serializer() {
        return serializerImpl;
    }


    protected void write(final JChannel channel, final JRequest request, final DispatchType dispatchType) {
        try {
            chains.doFilter(request, channel);
        } catch (Throwable e) {
            log.error("Job Schedule error, channel: {}", channel);
        }
    }
}
