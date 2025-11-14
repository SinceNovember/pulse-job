package com.simple.pulsejob.admin.scheduler.processor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.simple.plusejob.serialization.Serializer;
import com.simple.plusejob.serialization.SerializerType;
import com.simple.plusejob.serialization.io.InputBuf;
import com.simple.pulsejob.admin.common.model.entity.JobExecutor;
import com.simple.pulsejob.admin.persistence.mapper.JobExecutorMapper;
import com.simple.pulsejob.admin.scheduler.channel.ExecutorChannelGroupManager;
import com.simple.pulsejob.common.util.StringUtil;
import com.simple.pulsejob.common.util.Strings;
import com.simple.pulsejob.transport.JProtocolHeader;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.metadata.ExecutorKey;
import com.simple.pulsejob.transport.metadata.MessageWrapper;
import com.simple.pulsejob.transport.payload.JRequestPayload;
import com.simple.pulsejob.transport.payload.JResponsePayload;
import com.simple.pulsejob.transport.processor.AcceptorProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobExecutorAcceptorProcessor implements AcceptorProcessor {

    // 执行器名称到Channel的映射
    private static final ExecutorChannelGroupManager channelGroupManager = new ExecutorChannelGroupManager();

    private final Map<Byte, Serializer> serializerMap;

    private final JobExecutorMapper jobExecutorMapper;

    // 连接数量限制
    private static final int MAX_CONNECTIONS = 1000;

    // 连接超时时间
    private static final long CONNECTION_TIMEOUT_MS = 30000;


    @Override
    public void handleResponse(JChannel channel, JResponsePayload response) {
    }

    /**
     * 处理client发来的请求，这边主要处理注册执行器请求
     * @param channel
     * @param request
     */
    @Override
    public void handleRequest(JChannel channel, JRequestPayload request) {
        Serializer serializer = serializerMap.get(SerializerType.JAVA.value());
        InputBuf inputBuf = request.inputBuf();
        if (JProtocolHeader.REGISTER_EXECUTOR == request.messageCode()) {
            ExecutorKey executorWrapper = serializer.readObject(inputBuf, ExecutorKey.class);
            handleExecutorRegister(channel, executorWrapper);
        } else {
            try {
                MessageWrapper messageWrapper = serializer.readObject(inputBuf, MessageWrapper.class);
            } catch (Exception e) {
                log.error("[pulse-job] handle request failed", e);
            }
        }

    }

    @Override
    public void handleActive(JChannel channel) {
        try {
            // 检查连接数量限制
            if (channelGroupManager.size() >= MAX_CONNECTIONS) {
                log.warn("Maximum connections reached, rejecting: {}", channel.remoteAddress());
                channel.close();
                return;
            }

            log.info("New connection: {}, total connections: {}",
                channel.remoteAddress(), channelGroupManager.size());

        } catch (Exception e) {
            log.error("Error handling active channel: {}", channel.remoteAddress(), e);
            channel.close();
        }
    }

    @Override
    public void handleInactive(JChannel channel) {
    }

    @Override
    public void shutdown() {
    }

    public static ExecutorChannelGroupManager channelGroupManager() {
        return channelGroupManager;
    }

    /**
     * 处理执行器注册
     */
    private void handleExecutorRegister(JChannel channel, ExecutorKey executorKey) {
        if (!isValidExecutorWrapper(executorKey)) {
            return;
        }
        autoRegisterJobExecutor(channel, executorKey);
        channelGroupManager.find(executorKey).add(channel, () -> deregisterJobExecutor(executorWrapper, channel));
    }

    private boolean isValidExecutorWrapper(ExecutorKey executorWrapper) {
        return !StringUtil.isBlank(executorWrapper.getExecutorName());
    }

    public void autoRegisterJobExecutor(JChannel channel, ExecutorKey executorWrapper) {
        log.info("auto register job executor channel : {}", channel);
        final String executorName = executorWrapper.getExecutorName();
        String channelAddress = channel.remoteIpPort();

        JobExecutor jobExecutor = jobExecutorMapper.findByExecutorName(executorName)
            .map(existing -> existing.updateAddressIfAbsent(channelAddress))
            .orElseGet(() -> JobExecutor.of(executorName, channelAddress));

        jobExecutor.refreshUpdateTime();
        jobExecutorMapper.save(jobExecutor);
    }

    public void deregisterJobExecutor(ExecutorKey executorWrapper, JChannel channel) {
        final String executorName = executorWrapper.getExecutorName();
        jobExecutorMapper.findByExecutorName(executorName)
            .ifPresent(jobExecutor -> {
                String ipPort = channel.remoteIpPort();
                String address = jobExecutor.getExecutorAddress();
                if (StringUtil.isBlank(address)) {
                    return;
                }

                List<String> addressList = Arrays.stream(address.split(Strings.SEMICOLON))
                    .map(String::trim)
                    .filter(addr -> !addr.equals(ipPort))
                    .collect(Collectors.toList());

                jobExecutor.setExecutorAddress(String.join(Strings.SEMICOLON, addressList));
                jobExecutorMapper.save(jobExecutor);
            });
    }

}
