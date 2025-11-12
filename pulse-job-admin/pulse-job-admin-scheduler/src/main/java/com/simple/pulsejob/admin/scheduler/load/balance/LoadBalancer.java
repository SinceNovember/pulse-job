package com.simple.pulsejob.admin.scheduler.load.balance;

import com.simple.pulsejob.transport.channel.CopyOnWriteGroupList;
import com.simple.pulsejob.transport.channel.JChannelGroup;
import com.simple.pulsejob.transport.metadata.ExecutorKey;

public interface LoadBalancer {

    JChannelGroup select(CopyOnWriteGroupList groups, ExecutorKey executorKey);

}
