package com.simple.pulsejob.client.processor.task;

import com.simple.plusejob.serialization.Serializer;
import com.simple.plusejob.serialization.io.InputBuf;
import com.simple.pulsejob.client.JRequest;
import com.simple.pulsejob.client.JobContext;
import com.simple.pulsejob.client.model.metadata.MessageWrapper;
import com.simple.pulsejob.client.processor.DefaultClientProcessor;
import com.simple.pulsejob.client.registry.JobBeanDefinition;
import com.simple.pulsejob.common.concurrent.executor.reject.RejectedRunnable;
import com.simple.pulsejob.common.util.Reflects;
import com.simple.pulsejob.common.util.internal.logging.InternalLogger;
import com.simple.pulsejob.common.util.internal.logging.InternalLoggerFactory;
import com.simple.pulsejob.transport.CodecConfig;
import com.simple.pulsejob.transport.Status;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.payload.JRequestPayload;

import java.util.Map;
import java.util.Optional;

public class MessageTask implements RejectedRunnable {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MessageTask.class);

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

            // 反序列化
            if (CodecConfig.isCodecLowCopy()) {
                messageWrapper = serializer.readObject(requestPayload.inputBuf(), MessageWrapper.class);
            } else {
                messageWrapper = serializer.readObject(requestPayload.bytes(), MessageWrapper.class);
            }

            requestPayload.clear();
            request.setMessage(messageWrapper);
        } catch (Throwable t) {
            logger.error("Failed to deserialize request from {}: {}", channel.remoteAddress(), t.getMessage(), t);
            return;
        }

        JobBeanDefinition jobBeanDefinition = processor.getJobBeanDefinition(messageWrapper.getJobBeanDefinitionName());
        if (jobBeanDefinition == null) {
            logger.error("JobBeanDefinition not found: {}", messageWrapper.getJobBeanDefinitionName());
            handleException(Status.SERVICE_NOT_FOUND, new IllegalArgumentException(
                "JobBeanDefinition not found: " + messageWrapper.getJobBeanDefinitionName()));
            return;
        }

        JobContext jobContext = new JobContext(channel, request, messageWrapper, jobBeanDefinition);
        processor.invoke(jobContext);
    }


    public void handleException(Status status, Throwable cause) {
        processor.handleException(channel, request, status, cause);
    }

    @Override
    public void rejected() {

    }
}