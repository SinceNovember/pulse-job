package com.simple.pulsejob.admin.scheduler.channel;

import java.util.concurrent.ConcurrentMap;
import com.simple.pulsejob.common.util.Maps;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.channel.JChannelGroup;
import com.simple.pulsejob.transport.metadata.ExecutorKey;
import com.simple.pulsejob.transport.netty.channel.NettyChannelGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutorChannelGroupManager {

    // key: 执行器名称; value: 对应的执行器节点组
    private final ConcurrentMap<ExecutorKey, JChannelGroup> groups = Maps.newConcurrentMap();

    public JChannelGroup find(ExecutorKey executorKey) {
        JChannelGroup groupList = groups.get(executorKey);
        if (groupList == null) {
            JChannelGroup newGroupList = new NettyChannelGroup(null);
            groupList = groups.putIfAbsent(executorKey, newGroupList);
            if (groupList == null) {
                groupList = newGroupList;
            }
        }
        return groupList;
    }

    public int size() {
        int channelCount = 0;
        for (JChannelGroup channelGroup : groups.values()) {
            channelCount += channelGroup.size();
        }
        return channelCount;
    }

    public void removeChannel(ExecutorKey executorKey, JChannel channel) {
        JChannelGroup groupList = groups.get(executorKey);
        if (groupList != null) {
            groupList.remove(channel);
        }
    }

}
