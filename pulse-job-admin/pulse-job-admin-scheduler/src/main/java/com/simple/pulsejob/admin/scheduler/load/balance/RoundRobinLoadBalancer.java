package com.simple.pulsejob.admin.scheduler.load.balance;

import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.channel.JChannelGroup;
import org.springframework.stereotype.Component;

@Component
public class RoundRobinLoadBalancer implements LoadBalancer {

    @Override
    public JChannel select(JChannelGroup channelGroup) {
        return channelGroup.next();
    }

}
