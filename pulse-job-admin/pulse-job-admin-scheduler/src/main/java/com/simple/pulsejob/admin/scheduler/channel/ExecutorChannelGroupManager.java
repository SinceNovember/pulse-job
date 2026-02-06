package com.simple.pulsejob.admin.scheduler.channel;

import com.simple.pulsejob.common.util.Maps;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.channel.JChannelGroup;
import com.simple.pulsejob.transport.metadata.ExecutorKey;
import com.simple.pulsejob.transport.netty.channel.NettyChannelGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

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

    /**
     * 关闭指定执行器名称下的所有 Channel 并移除该 group
     * <p>Channel 关闭后会自动触发 closeFuture 回调（preCloseProcessor + group 移除）</p>
     *
     * @param executorName 执行器名称
     * @return 被关闭的 Channel 数量
     *
     * AI Generated
     */
    public int closeAndRemoveByName(String executorName) {
        if (executorName == null || executorName.isEmpty()) {
            return 0;
        }

        JChannelGroup group = groups.remove(executorName);
        if (group == null || group.isEmpty()) {
            log.info("No active channels found for executor: {}", executorName);
            return 0;
        }

        List<JChannel> channels = group.channels();
        int count = channels.size();
        log.info("Closing {} channel(s) for executor: {}", count, executorName);

        for (JChannel channel : channels) {
            try {
                if (channel.isActive()) {
                    channel.close();
                    log.info("Closed channel: {} for executor: {}", channel.remoteAddress(), executorName);
                }
            } catch (Exception e) {
                log.warn("Failed to close channel for executor {}: {}", executorName, e.getMessage());
            }
        }

        return count;
    }
}
