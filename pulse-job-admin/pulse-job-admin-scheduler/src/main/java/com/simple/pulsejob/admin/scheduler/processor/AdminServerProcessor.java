package com.simple.pulsejob.admin.scheduler.processor;

import com.simple.plusejob.serialization.Serializer;
import com.simple.plusejob.serialization.SerializerType;
import com.simple.plusejob.serialization.io.InputBuf;
import com.simple.pulsejob.admin.common.model.enums.JobInstanceStatus;
import com.simple.pulsejob.admin.persistence.mapper.JobInstanceMapper;
import com.simple.pulsejob.admin.scheduler.ExecutorRegistryService;
import com.simple.pulsejob.admin.scheduler.channel.ExecutorChannelGroupManager;
import com.simple.pulsejob.admin.scheduler.factory.SerializerFactory;
import com.simple.pulsejob.admin.scheduler.future.DefaultInvokeFuture;
import com.simple.pulsejob.admin.scheduler.log.JobLogDispatcher;
import com.simple.pulsejob.admin.websocket.service.WebSocketBroadcastService;
import com.simple.pulsejob.common.util.StringUtil;
import com.simple.pulsejob.transport.JResponse;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.metadata.BatchLogMessage;
import com.simple.pulsejob.transport.metadata.ExecutorKey;
import com.simple.pulsejob.transport.metadata.LogMessage;
import com.simple.pulsejob.transport.metadata.ResultWrapper;
import com.simple.pulsejob.transport.payload.JRequestPayload;
import com.simple.pulsejob.transport.payload.JResponsePayload;
import com.simple.pulsejob.transport.processor.AcceptorProcessor;
import lombok.RequiredArgsConstructor;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.simple.pulsejob.transport.JProtocolHeader.*;

/**
 * Admin 服务端消息处理器.
 *
 * <p>处理来自 Executor 客户端的消息：</p>
 * <ul>
 *   <li>执行器注册</li>
 *   <li>任务日志（单条/批量）</li>
 *   <li>任务执行结果</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminServerProcessor implements AcceptorProcessor {

    private final ExecutorChannelGroupManager channelGroupManager;
    private final ExecutorRegistryService executorRegistryService;
    private final SerializerFactory serializerFactory;
    private final JobLogDispatcher jobLogDispatcher;
    private final WebSocketBroadcastService broadcastService;

    /** 缓存默认序列化器 */
    private volatile Serializer defaultSerializer;

    // ==================== AcceptorProcessor 实现 ====================

    @Override
    public void handleRequest(JChannel channel, JRequestPayload request) {
        Serializer serializer = getSerializer(request.serializerCode());
        if (serializer == null) {
            log.error("No serializer found for code={}", request.serializerCode());
            return;
        }

        InputBuf inputBuf = request.inputBuf();
        byte messageCode = request.messageCode();
        switch (messageCode) {
            case REGISTER_EXECUTOR:
                handleRegisterExecutor(channel, serializer, inputBuf);
                break;
//            case JOB_LOG_MESSAGE:
//                handleLogMessage(channel, request.instanceId(), serializer, inputBuf);
//                break;
//            case JOB_BATCH_LOG_MESSAGE:
//                handleBatchLogMessage(channel, serializer, inputBuf);
//                break;
//            case JOB_RESULT:
//                handleJobResult(channel, request.instanceId(), serializer, inputBuf);
//                break;
            default:
                log.warn("Unknown request message code: {}", messageCode);
                break;
        }
    }

    @Override
    public void handleResponse(JChannel channel, JResponsePayload response) {
        Serializer serializer = getSerializer(response.serializerCode());
        if (serializer == null) {
            log.error("No serializer found for code={}", response.serializerCode());
            return;
        }

        InputBuf inputBuf = response.inputBuf();
        byte messageCode = response.messageCode();

        switch (messageCode) {
            case JOB_BATCH_LOG_MESSAGE:
                handleBatchLogMessage(channel, serializer, inputBuf);
                break;
            case JOB_LOG_MESSAGE:
                handleLogMessage(channel, response.instanceId() , serializer, inputBuf);
                break;
            case JOB_RESULT:
                handleJobResult(channel, response, serializer, inputBuf);
                break;
            default:
                log.debug("Unhandled response message code: {}", messageCode);
                break;
        }
    }

    @Override
    public void handleActive(JChannel channel) {
        log.info("Executor connected: {}", channel.remoteAddress());
    }

    @Override
    public void handleInactive(JChannel channel) {
        String remoteAddress = channel.remoteAddress() != null ? channel.remoteAddress().toString() : "unknown";
        log.info("Executor disconnected: {}", remoteAddress);
        
        // ✅ 清理该 Channel 上所有未完成的 Future（比等超时更快响应）
        List<Long> affectedInstanceIds = DefaultInvokeFuture.onChannelInactive(channel);
        
        // 注意：执行器下线广播在 ExecutorRegistryService.deregister() 中处理
        // 因为 preCloseProcessor 回调会在 handleInactive 之前执行
    }

    @Override
    public void shutdown() {
        log.info("AdminServerProcessor shutting down...");
    }

    // ==================== 消息处理 ====================

    /**
     * 处理执行器注册
     */
    private void handleRegisterExecutor(JChannel channel, Serializer serializer, InputBuf inputBuf) {
        ExecutorKey executorKey = serializer.readObject(inputBuf, ExecutorKey.class);

        if (!isValidExecutorKey(executorKey)) {
            log.warn("Invalid executor register request: {}", executorKey);
            return;
        }

        log.info("Executor registering: name={}, address={}",
                executorKey.getExecutorName(), channel.remoteAddress());

        // 注册到内存
        channelGroupManager.add(executorKey, channel,
                () -> executorRegistryService.deregister(executorKey, channel));

        // 持久化
        executorRegistryService.register(executorKey, channel);
        broadcastService.pushExecutorOnline(executorKey.getExecutorName(), channel.remoteAddress().toString());
    }

    /**
     * 处理单条日志消息
     */
    private void handleLogMessage(JChannel channel, long instanceId, Serializer serializer, InputBuf inputBuf) {
        Object payload = serializer.readObject(inputBuf, Object.class);
        dispatchLogPayload(channel, instanceId, payload);
    }

    /**
     * 处理批量日志消息
     */
    private void handleBatchLogMessage(JChannel channel, Serializer serializer, InputBuf inputBuf) {
        BatchLogMessage batch = serializer.readObject(inputBuf, BatchLogMessage.class);

        if (batch == null || batch.isEmpty()) {
            return;
        }

        log.debug("Received batch log: count={}", batch.size());

        List<LogMessage> logs = batch.getLogs();

        // 转发到 Future（实时推送）
//        for (LogMessage logMessage : logs) {
//            Long instanceId = logMessage.getInstanceId();
//            if (instanceId != null) {
//                DefaultInvokeFuture.receivedLog(channel, instanceId, logMessage);
//            }
//        }

        // 派发到所有监听器（持久化等）
        jobLogDispatcher.dispatchBatch(logs);
    }

    /**
     * 处理任务执行结果
     */
    private void handleJobResult(JChannel channel, JResponsePayload payload, Serializer serializer, InputBuf inputBuf) {
        ResultWrapper resultWrapper = serializer.readObject(inputBuf, ResultWrapper.class);
        JResponse response = new JResponse(payload);
        response.result(resultWrapper);

        DefaultInvokeFuture.received(channel, response);
    }

    /**
     * 分发日志（单条）
     */
    private void dispatchLogPayload(JChannel channel, long instanceId, Object payload) {
        if (payload instanceof LogMessage) {
            processLogMessage(channel, instanceId, (LogMessage) payload);
            return;
        }

        log.warn("Unknown log payload type: {}", payload != null ? payload.getClass() : "null");
    }

    /**
     * 处理单条日志
     */
    private void processLogMessage(JChannel channel, long instanceId, LogMessage logMessage) {
        log.debug("Received log: instanceId={}, level={}", instanceId, logMessage.getLevel());

        // 转发到 Future（实时推送）
        DefaultInvokeFuture.receivedLog(channel, instanceId, logMessage);

        // 派发到所有监听器（持久化等）
        jobLogDispatcher.dispatch(logMessage);
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取序列化器（带缓存）
     */
    private Serializer getSerializer(byte code) {
        // 大多数情况使用 JAVA 序列化
        if (code == SerializerType.JAVA.value()) {
            if (defaultSerializer == null) {
                defaultSerializer = serializerFactory.get(SerializerType.JAVA);
            }
            return defaultSerializer;
        }

        SerializerType type = SerializerType.parse(code);
        if (type == null) {
            return null;
        }
        return serializerFactory.get(type);
    }

    private boolean isValidExecutorKey(ExecutorKey key) {
        return key != null && StringUtil.isNotBlank(key.getExecutorName());
    }
}
