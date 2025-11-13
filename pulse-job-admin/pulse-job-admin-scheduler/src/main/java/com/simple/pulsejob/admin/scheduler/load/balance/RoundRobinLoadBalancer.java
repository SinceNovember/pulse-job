package com.simple.pulsejob.admin.scheduler.load.balance;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import com.simple.pulsejob.transport.channel.CopyOnWriteGroupList;
import com.simple.pulsejob.transport.channel.JChannelGroup;
import com.simple.pulsejob.transport.metadata.ExecutorKey;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Scope("prototype")
@Component("roundRobinLoadBalancer")
public class RoundRobinLoadBalancer implements LoadBalancer {

    private static final AtomicIntegerFieldUpdater<RoundRobinLoadBalancer> indexUpdater =
        AtomicIntegerFieldUpdater.newUpdater(RoundRobinLoadBalancer.class, "index");

    private volatile int index = 0;

    @Override
    public JChannelGroup select(CopyOnWriteGroupList groups, ExecutorKey executorKey) {
        JChannelGroup[] elements = groups.getSnapshot();
        int length = elements.length;

        if (length == 0) {
            return null;
        }

        if (length == 1) {
            return elements[0];
        }
        int rrIndex = indexUpdater.getAndIncrement(this) & Integer.MAX_VALUE;
        return elements[rrIndex];
    }

}
