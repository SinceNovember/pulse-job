package com.simple.pulsejob.admin.processor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.simple.plusejob.serialization.Serializer;
import com.simple.plusejob.serialization.SerializerType;
import com.simple.plusejob.serialization.io.InputBuf;
import com.simple.pulsejob.admin.service.IJobExecutorService;
import com.simple.pulsejob.common.util.StringUtil;
import com.simple.pulsejob.transport.JProtocolHeader;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.channel.JChannelGroup;
import com.simple.pulsejob.transport.metadata.JobExecutorWrapper;
import com.simple.pulsejob.transport.metadata.MessageWrapper;
import com.simple.pulsejob.transport.netty.channel.NettyChannel;
import com.simple.pulsejob.transport.payload.JRequestPayload;
import com.simple.pulsejob.transport.payload.JResponsePayload;
import com.simple.pulsejob.transport.processor.AcceptorProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultAcceptorProcessor implements AcceptorProcessor {

    public static final List<JChannel> CLIENT_CHANNELS = new ArrayList<>();

    public static final Map<String, List<JChannelGroup>> EXECUTOR_CHANNEL_GROUPS = new HashMap<>();

    // 执行器名称到Channel的映射
    private static final Map<String, JChannel> EXECUTOR_CHANNELS = new HashMap<>();

    private final Map<Byte, Serializer> serializerMap;

    private final IJobExecutorService jobExecutorService;

    @Override
    public void handleResponse(JChannel channel, JResponsePayload response) {
        System.out.println(response);
    }

    /**
     * 处理client发来的请求，这边主要处理注册执行器请求
     * @param channel
     * @param request
     */
    @Override
    public void handleRequest(JChannel channel, JRequestPayload request) {
        System.out.println(request);
        // 反序列化消息
        Serializer serializer = serializerMap.get(SerializerType.JAVA.value());
        InputBuf inputBuf = request.inputBuf();
        if (JProtocolHeader.REGISTER_EXECUTOR == request.messageCode()) {
            JobExecutorWrapper executorWrapper = serializer.readObject(inputBuf, JobExecutorWrapper.class);
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
        log.info("[pulse-job] new connection: {}", channel.remoteAddress());
        CLIENT_CHANNELS.add(channel);
    }

    @Override
    public void handleInactive(JChannel channel) {
        log.info("[pulse-job] connection closed: {}", channel.remoteAddress());
        CLIENT_CHANNELS.remove(channel);
    }

    @Override
    public void shutdown() {
        EXECUTOR_CHANNELS.clear();
        CLIENT_CHANNELS.clear();
    }

    /**
     * 处理执行器注册
     */
    private void handleExecutorRegister(JChannel channel, JobExecutorWrapper executorWrapper) {
        jobExecutorService.autoRegisterJobExecutor(channel, executorWrapper);
    }

    /**
     * 根据执行器名称获取Channel
     */
    public static JChannel getChannelByExecutorName(String executorName) {
        return EXECUTOR_CHANNELS.get(executorName);
    }
}
