package com.simple.pulsejob.admin.scheduler.processor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import static com.simple.pulsejob.transport.JProtocolHeader.*;
import com.simple.plusejob.serialization.Serializer;
import com.simple.plusejob.serialization.SerializerType;
import com.simple.plusejob.serialization.io.InputBuf;
import com.simple.pulsejob.admin.common.model.entity.JobExecutor;
import com.simple.pulsejob.admin.persistence.mapper.JobExecutorMapper;
import com.simple.pulsejob.admin.scheduler.ExecutorRegistryService;
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
    private final ExecutorChannelGroupManager channelGroupManager;

    private final ExecutorRegistryService executorRegistryService;

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
        if (serializer == null) {
            log.error("No serializer found for code={}", request.serializerCode());
            return;
        }


        final InputBuf inputBuf = request.inputBuf();
        switch (request.messageCode()) {
            case REGISTER_EXECUTOR:
                ExecutorKey executorKey = serializer.readObject(inputBuf, ExecutorKey.class);
                registerExecutor(channel, executorKey);
                break;
            case TRIGGER_JOB:

            default:
                MessageWrapper messageWrapper = serializer.readObject(inputBuf, MessageWrapper.class);


        }

    }

    @Override
    public void handleActive(JChannel channel) {

    }

    @Override
    public void handleInactive(JChannel channel) {
    }

    @Override
    public void shutdown() {
    }

    /**
     * 处理执行器注册
     */
    private void registerExecutor(JChannel channel, ExecutorKey executorKey) {
        if (!isValidExecutorWrapper(executorKey)) {
            log.warn("Invalid executor register request: {}", executorKey);
            return;
        }
        //注册到内存的 channel group
        channelGroupManager.add(executorKey, channel, () -> executorRegistryService.deregister(executorKey, channel));

        //持久化注册信息
        executorRegistryService.register(executorKey, channel);
    }

    private boolean isValidExecutorWrapper(ExecutorKey executorWrapper) {
        return !StringUtil.isBlank(executorWrapper.getExecutorName());
    }

}
