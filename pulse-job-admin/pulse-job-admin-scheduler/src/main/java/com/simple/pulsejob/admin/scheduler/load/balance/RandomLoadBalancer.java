package com.simple.pulsejob.admin.scheduler.load.balance;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.channel.JChannelGroup;
import org.springframework.stereotype.Component;

@Component
public class RandomLoadBalancer implements LoadBalancer {
    @Override
    public JChannel select(JChannelGroup channelGroup) {
        List<? extends JChannel> channels = channelGroup.channels();
        int length = channels.size();

        if (length == 0) {
            return null;
        }

        if (length == 1) {
            return channels.get(0);
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        return channels.get(random.nextInt(length));
    }
}
