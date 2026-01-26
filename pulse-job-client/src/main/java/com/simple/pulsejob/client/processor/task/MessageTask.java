package com.simple.pulsejob.client.processor.task;

import com.simple.plusejob.serialization.Serializer;
import com.simple.plusejob.serialization.SerializerType;
import com.simple.pulsejob.client.JobContext;
import com.simple.pulsejob.client.processor.DefaultClientProcessor;
import com.simple.pulsejob.common.concurrent.executor.reject.RejectedRunnable;
import com.simple.pulsejob.transport.CodecConfig;
import com.simple.pulsejob.transport.JProtocolHeader;
import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.Status;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.channel.JFutureListener;
import com.simple.pulsejob.transport.metadata.MessageWrapper;
import com.simple.pulsejob.transport.metadata.ResultWrapper;
import com.simple.pulsejob.transport.payload.JRequestPayload;
import com.simple.pulsejob.transport.payload.JResponsePayload;
import com.simple.pulsejob.transport.payload.PayloadSerializer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 任务执行消息处理任务.
 *
 * <p>负责：</p>
 * <ul>
 *   <li>反序列化请求消息</li>
 *   <li>调用对应的 Job 处理器</li>
 *   <li>序列化并发送响应</li>
 * </ul>
 */
@Slf4j
public class MessageTask implements RejectedRunnable {

    /** 
     * JSON 序列化器（线程安全，复用实例）
     * 用于将任务返回值转为 JSON 字符串，避免用户对象不实现 Serializable 导致的序列化异常
     * 
     * 配置说明：
     * - 允许访问私有字段（用户类可能没有 getter）
     * - 禁用空 Bean 检查（避免无字段/无 getter 的类报错）
     */
    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();
    
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // 允许序列化私有字段（无需 getter）
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        // 禁用空 Bean 序列化异常
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        return mapper;
    }

    private final DefaultClientProcessor processor;
    private final JChannel channel;
    private final JRequest request;

    public MessageTask(DefaultClientProcessor processor, JChannel channel, JRequest request) {
        this.processor = processor;
        this.channel = channel;
        this.request = request;
    }

    @Override
    public void run() {
        MessageWrapper messageWrapper;
        try {
            JRequestPayload requestPayload = request.getPayload();
            byte serializerCode = requestPayload.serializerCode();

            Serializer serializer = Optional.ofNullable(processor.serializer(serializerCode))
                    .orElseThrow(() -> new IllegalArgumentException("Unknown serializer code: " + serializerCode));

            if (CodecConfig.isCodecLowCopy()) {
                messageWrapper = serializer.readObject(requestPayload.inputBuf(), MessageWrapper.class);
            } else {
                messageWrapper = serializer.readObject(requestPayload.bytes(), MessageWrapper.class);
            }

            requestPayload.clear();
            request.setMessage(messageWrapper);
        } catch (Throwable t) {
            log.error("Failed to deserialize request from {}: {}", channel.remoteAddress(), t.getMessage(), t);
            // 反序列化失败也要及时回报给管理端
            processor.handleRequestException(channel, request, Status.DESERIALIZATION_FAIL, t);
            return;
        }

        process(JobContext.of(channel, request, messageWrapper));
    }

    @Override
    public void rejected() {
        log.warn("Task rejected, instanceId: {}, channel: {}", request.instanceId(), channel.remoteAddress());
        processor.handleRequestException(channel, request, Status.SERVER_BUSY,
                new RuntimeException("Server busy, task rejected"));
    }

    /**
     * 处理任务（带超时控制）
     */
    private void process(JobContext jobContext) {
        int timeoutSeconds = jobContext.getTimeoutSeconds();
        
        if (timeoutSeconds > 0) {
            // 有超时限制：使用 CompletableFuture 包装执行
            processWithTimeout(jobContext, timeoutSeconds);
        } else {
            // 无超时限制：直接执行
            processWithoutTimeout(jobContext);
        }
    }

    /**
     * 无超时控制的任务执行
     */
    private void processWithoutTimeout(JobContext jobContext) {
        try {
            Object invokeResult = processor.invoke(jobContext);
            doProcess(invokeResult);
        } catch (Throwable t) {
            handleFail(t);
        }
    }

    /**
     * 带超时控制的任务执行
     */
    private void processWithTimeout(JobContext jobContext, int timeoutSeconds) {
        // 使用 CompletableFuture 包装任务执行
        CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
            try {
                return processor.invoke(jobContext);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        });

        try {
            // 等待执行结果，超时则抛出 TimeoutException
            Object result = future.get(timeoutSeconds, TimeUnit.SECONDS);
            doProcess(result);
        } catch (TimeoutException e) {
            // ⚠️ 执行超时
            log.warn("Task execution timeout after {}s, instanceId={}, handler={}", 
                    timeoutSeconds, request.instanceId(), jobContext.getHandlerName());
            
            // 尝试取消任务（如果任务还在执行）
            future.cancel(true);
            
            // 返回超时错误
            handleFail(new TimeoutException(
                    "Task execution timeout after " + timeoutSeconds + " seconds"), Status.CLIENT_TIMEOUT);
        } catch (ExecutionException e) {
            // 执行异常
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof RuntimeException && cause.getCause() != null) {
                cause = cause.getCause();
            }
            handleFail(cause);
        } catch (InterruptedException e) {
            // 线程被中断
            Thread.currentThread().interrupt();
            handleFail(e);
        }
    }

    private void doProcess(Object result) {
        ResultWrapper wrapper = new ResultWrapper();
        // 框架层自动将返回值转为 JSON 字符串，避免用户对象序列化问题
        // 这样用户返回任意类型（不实现 Serializable）都不会报错
        if (result != null) {
            try {
                String jsonResult = OBJECT_MAPPER.writeValueAsString(result);
                wrapper.setResult(jsonResult);
            } catch (Exception e) {
                // JSON 转换失败（如循环引用），记录日志但不影响响应
                log.warn("Failed to serialize result to JSON, result will be null. instanceId={}, error={}", 
                        request.instanceId(), e.getMessage());
                wrapper.setResult(null);
            }
        }

        SerializerType type = SerializerType.parse(request.serializerCode());
        JResponsePayload response = PayloadSerializer.response()
                .instanceId(request.instanceId())
                .channel(channel)
                .type(type != null ? type : SerializerType.JAVA)
                .message(wrapper)
                .messageCode(JProtocolHeader.JOB_RESULT)
                .build();

        response.status(Status.OK.value());

        channel.write(response, ResponseListener.INSTANCE);
    }

    private void handleFail(Throwable t) {
        handleFail(t, Status.CLIENT_ERROR);
    }

    private void handleFail(Throwable t, Status status) {
        processor.handleRequestException(channel, request, status, t);
    }
    /**
     * 响应发送监听器（单例复用）
     */
    private enum ResponseListener implements JFutureListener<JChannel> {
        INSTANCE;

        @Override
        public void operationSuccess(JChannel channel) {
            log.debug("Response sent successfully to {}", channel.remoteAddress());
        }

        @Override
        public void operationFailure(JChannel channel, Throwable cause) {
            log.error("Response sent failed to {}: {}", channel.remoteAddress(), cause.getMessage());
        }
    }
}
