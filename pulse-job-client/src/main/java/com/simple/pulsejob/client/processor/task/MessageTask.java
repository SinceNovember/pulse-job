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
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

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
     * 处理任务
     */
    private void process(JobContext jobContext) {
        try {
            Object invokeResult = processor.invoke(jobContext);
            doProcess(invokeResult);
        } catch (Throwable t) {
            handleFail(jobContext, t);
        }
    }

    private void doProcess(Object result) {
        ResultWrapper wrapper = new ResultWrapper();
        wrapper.setResult(result);

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


    private void handleFail(JobContext jobContext, Throwable t) {
        processor.handleRequestException(channel, request, Status.CLIENT_ERROR, t);

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
