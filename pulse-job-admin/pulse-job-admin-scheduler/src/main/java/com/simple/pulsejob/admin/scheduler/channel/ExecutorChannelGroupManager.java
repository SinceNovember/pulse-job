package com.simple.pulsejob.admin.scheduler.channel;

import java.util.List;
import java.util.concurrent.ConcurrentMap;
import com.simple.pulsejob.common.util.Maps;
import com.simple.pulsejob.transport.channel.CopyOnWriteGroupList;
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
    private final ConcurrentMap<String, JChannelGroup> groups = Maps.newConcurrentMap();

    public JChannelGroup find(ExecutorKey executorKey) {
        String _executorKey = executorKey.exeuctorKeyString();
        return groups.computeIfAbsent(
            _executorKey, k -> new NettyChannelGroup());
    }

    public void add(ExecutorKey executorKey, JChannel channel, Runnable preCloseProcessor) {
        find(executorKey).add(channel, preCloseProcessor);
    }

    public void remove(ExecutorKey executorKey, JChannel channel) {
        find(executorKey).remove(channel);
    }

    /**
     * 根据 Channel 获取执行器名称
     * @param channel 连接通道
     * @return 执行器名称，如果找不到返回 null
     */
    public String getExecutorNameByChannel(JChannel channel) {
        if (channel == null) {
            return null;
        }
        
        for (var entry : groups.entrySet()) {
            JChannelGroup group = entry.getValue();
            if (group != null) {
                List<JChannel> channels = group.channels();
                if (channels != null) {
                    for (JChannel ch : channels) {
                        if (ch != null && ch.equals(channel)) {
                            return entry.getKey();
                        }
                    }
                }
            }
        }
        return null;
    }
}
