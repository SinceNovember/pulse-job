package com.simple.pulsejob.admin.scheduler.load.balance;

import com.simple.pulsejob.transport.channel.CopyOnWriteGroupList;
import com.simple.pulsejob.transport.channel.JChannelGroup;
import com.simple.pulsejob.transport.metadata.ExecutorKey;

public class RandomLoadBalancer implements LoadBalancer{
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
        return null;
    }
}
