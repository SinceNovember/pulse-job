package com.simple.pulsejob.admin.scheduler;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.simple.pulsejob.admin.common.model.entity.JobExecutor;
import com.simple.pulsejob.admin.persistence.mapper.JobExecutorMapper;
import com.simple.pulsejob.admin.websocket.service.WebSocketBroadcastService;
import com.simple.pulsejob.common.util.Strings;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.metadata.ExecutorKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutorRegistryService {

    private final JobExecutorMapper jobExecutorMapper;
    private final WebSocketBroadcastService broadcastService;

    /**
     * 注册执行器：将 channel 地址追加到 JobExecutor 的 address 列表（幂等）
     */
    public void register(ExecutorKey executorKey, JChannel channel) {
        if (executorKey == null || channel == null) return;

        final String executorName = executorKey.getExecutorName();
        final String ipPort = channel.remoteIpPort();

        jobExecutorMapper.findByExecutorName(executorName)
            .map(existing -> {
                // 如果不存在该地址才追加
                String addr = existing.getExecutorAddress(); // ; 分隔
                if (addr == null || addr.trim().isEmpty()) {
                    existing.setExecutorAddress(ipPort);
                    existing.refreshUpdateTime();
                    jobExecutorMapper.save(existing);
                } else {
                    List<String> addresses = Arrays.stream(addr.split(Strings.SEMICOLON))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
                    if (!addresses.contains(ipPort)) {
                        addresses.add(ipPort);
                        existing.setExecutorAddress(String.join(Strings.SEMICOLON, addresses));
                        existing.refreshUpdateTime();
                        jobExecutorMapper.save(existing);
                    } else {
                        // already present, just refresh timestamp
                        existing.refreshUpdateTime();
                        jobExecutorMapper.save(existing);
                    }
                }
                return existing;
            })
            .orElseGet(() -> {
                JobExecutor newOne = JobExecutor.of(executorName, ipPort);
                newOne.refreshUpdateTime();
                jobExecutorMapper.save(newOne);
                return newOne;
            });

        log.info("Registered executor persistent info: {} -> {}", executorName, ipPort);
    }

    /**
     * 注销 channel：从 JobExecutor 的 address 列表中移除该 channel 的 address（幂等）
     */
    public void deregister(ExecutorKey executorKey, JChannel channel) {
        if (executorKey == null || channel == null) return;

        final String executorName = executorKey.getExecutorName();
        final String ipPort = channel.remoteIpPort();

        jobExecutorMapper.findByExecutorName(executorName)
            .ifPresent(exec -> {
                String address = exec.getExecutorAddress();
                if (address == null || address.trim().isEmpty()) return;
                List<String> addresses = Arrays.stream(address.split(Strings.SEMICOLON))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty() && !s.equals(ipPort))
                    .collect(Collectors.toList());
                exec.setExecutorAddress(String.join(Strings.SEMICOLON, addresses));
                exec.refreshUpdateTime();
                jobExecutorMapper.save(exec);
                log.info("Deregistered executor persistent info: {} - removed {}", executorName, ipPort);
                
                // 广播执行器下线消息给浏览器（包含地址信息，以便前端实时更新地址列表）
                broadcastService.pushExecutorOffline(executorName, ipPort, "Connection closed");
                log.info("Broadcast executor offline: executorName={}, address={}", executorName, ipPort);
            });
    }
}
