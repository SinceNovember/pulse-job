package com.simple.pulsejob.admin.scheduler.load.balance;

import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.channel.JChannelGroup;

public interface LoadBalancer {

    JChannel select(JChannelGroup channelGroup);

    Type type();

    enum Type {
        ROUND,
        RANDOM
    }

}
