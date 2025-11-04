package com.simple.pulsejob.admin.scheduler.channel;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import com.simple.pulsejob.common.util.Maps;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.channel.JChannelGroup;
import com.simple.pulsejob.transport.metadata.JobExecutorWrapper;
import com.simple.pulsejob.transport.netty.channel.NettyChannelGroup;

public class ExecutorJChannelGroup {

    // key: 执行器名称; value: 对应的执行器节点组
    private final ConcurrentMap<String, JChannelGroup> groups = Maps.newConcurrentMap();

    public JChannelGroup find(JobExecutorWrapper executorWrapper) {
        final String executorName = executorWrapper.getExecutorName();
        JChannelGroup groupList = groups.get(executorName);
        if (groupList == null) {
            JChannelGroup newGroupList = new NettyChannelGroup(null);
            groupList = groups.putIfAbsent(executorName, newGroupList);
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

    public void removeChannel(String executorName, JChannel channel) {
        JChannelGroup groupList = groups.get(executorName);
        if (groupList != null) {
            groupList.remove(channel);
        }
    }

}
