package com.simple.pulsejob.admin.scheduler;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import com.simple.pulsejob.admin.common.model.entity.JobExecutor;
import com.simple.pulsejob.admin.common.model.entity.SystemConfig;
import com.simple.pulsejob.admin.persistence.mapper.JobExecutorMapper;
import com.simple.pulsejob.admin.persistence.mapper.SystemConfigMapper;
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
    private final SystemConfigMapper systemConfigMapper;
    
    // 按 executorName 加锁，避免并发注册问题
    private final ConcurrentMap<String, Object> locks = new ConcurrentHashMap<>();

    private Object getLock(String executorName) {
        return locks.computeIfAbsent(executorName, k -> new Object());
    }

    /**
     * 注册执行器：更新执行器地址
     * 按完整地址（IP:Port）去重，支持同一台机器运行多个实例
     * 
     * 注意：如果系统配置禁用了执行器自动注册，则不会创建新的执行器记录，
     * 但会更新已存在的执行器地址。
     */
    public void register(ExecutorKey executorKey, JChannel channel) {
        if (executorKey == null || channel == null) return;

        final String executorName = executorKey.getExecutorName();
        // 优先使用业务地址，没有则用 channel 地址
        final String newAddress = executorKey.getExecutorAddress() != null 
                ? executorKey.getExecutorAddress() 
                : channel.remoteIpPort();

        // 同一个 executorName 的注册操作需要同步
        synchronized (getLock(executorName)) {
            Optional<JobExecutor> existingOpt = jobExecutorMapper.findByExecutorName(executorName);
            
            if (existingOpt.isPresent()) {
                // 执行器已存在，更新地址
                JobExecutor existing = existingOpt.get();
                String currentAddresses = existing.getExecutorAddress();
                if (currentAddresses == null || currentAddresses.trim().isEmpty()) {
                    // 没有地址，直接设置
                    existing.setExecutorAddress(newAddress);
                } else {
                    // 按完整地址去重
                    List<String> addresses = Arrays.stream(currentAddresses.split(Strings.SEMICOLON))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty() && !s.equals(newAddress))
                        .collect(Collectors.toList());
                    addresses.add(newAddress);  // 添加新地址
                    existing.setExecutorAddress(String.join(Strings.SEMICOLON, addresses));
                }
                existing.refreshUpdateTime();
                jobExecutorMapper.save(existing);
                log.info("Updated executor address: name={}, address={}", executorName, newAddress);
            } else {
                // 执行器不存在，检查是否允许自动注册
                if (isAutoRegisterExecutorEnabled()) {
                    JobExecutor newOne = JobExecutor.of(executorName, newAddress);
                    newOne.refreshUpdateTime();
                    jobExecutorMapper.save(newOne);
                    log.info("Auto-registered new executor: name={}, address={}", executorName, newAddress);
                } else {
                    log.warn("Executor auto-registration disabled, ignoring new executor: name={}, address={}", 
                            executorName, newAddress);
                }
            }
        }
    }

    /**
     * 注销 channel：从地址列表中移除对应的完整地址（IP:Port）
     */
    public void deregister(ExecutorKey executorKey, JChannel channel) {
        if (executorKey == null || channel == null) return;

        final String executorName = executorKey.getExecutorName();
        final String address = executorKey.getExecutorAddress() != null 
                ? executorKey.getExecutorAddress() 
                : channel.remoteIpPort();

        // 同一个 executorName 的操作需要同步
        synchronized (getLock(executorName)) {
            jobExecutorMapper.findByExecutorName(executorName)
                .ifPresent(exec -> {
                    String currentAddresses = exec.getExecutorAddress();
                    if (currentAddresses != null && !currentAddresses.trim().isEmpty()) {
                        // 按完整地址移除
                        List<String> addresses = Arrays.stream(currentAddresses.split(Strings.SEMICOLON))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty() && !s.equals(address))
                            .collect(Collectors.toList());
                        exec.setExecutorAddress(addresses.isEmpty() ? null : String.join(Strings.SEMICOLON, addresses));
                        exec.refreshUpdateTime();
                        jobExecutorMapper.save(exec);
                        log.info("Deregistered executor: {} - removed {}", executorName, address);
                    }
                    
                    // 广播执行器下线消息给浏览器
                    broadcastService.pushExecutorOffline(executorName, address, "Connection closed");
                });
        }
    }

    /**
     * 检查是否启用执行器自动注册
     * 直接从 persistence 层读取配置，避免依赖 business 层
     */
    private boolean isAutoRegisterExecutorEnabled() {
        return systemConfigMapper.findByConfigKey(SystemConfig.KEY_AUTO_REGISTER_EXECUTOR)
                .map(config -> {
                    String value = config.getConfigValue();
                    return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
                })
                .orElse(true); // 默认启用
    }
}

