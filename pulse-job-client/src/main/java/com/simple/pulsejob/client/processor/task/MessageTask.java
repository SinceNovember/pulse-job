package com.simple.pulsejob.client.processor.task;

import com.simple.plusejob.serialization.Serializer;
import com.simple.plusejob.serialization.io.InputBuf;
import com.simple.plusejob.serialization.io.OutputBuf;
import com.simple.pulsejob.client.JRequest;
import com.simple.pulsejob.client.JobContext;
import com.simple.pulsejob.client.model.metadata.MessageWrapper;
import com.simple.pulsejob.client.model.metadata.ResultWrapper;
import com.simple.pulsejob.client.processor.DefaultClientProcessor;
import com.simple.pulsejob.client.registry.JobBeanDefinition;
import com.simple.pulsejob.common.concurrent.executor.reject.RejectedRunnable;
import com.simple.pulsejob.common.util.Reflects;
import com.simple.pulsejob.common.util.Signal;
import com.simple.pulsejob.common.util.SystemClock;
import com.simple.pulsejob.common.util.internal.logging.InternalLogger;
import com.simple.pulsejob.common.util.internal.logging.InternalLoggerFactory;
import com.simple.pulsejob.transport.CodecConfig;
import com.simple.pulsejob.transport.Status;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.channel.JFutureListener;
import com.simple.pulsejob.transport.exception.IoSignals;
import com.simple.pulsejob.transport.payload.JRequestPayload;
import com.simple.pulsejob.transport.payload.JResponsePayload;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

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

            // 反序列化
            if (CodecConfig.isCodecLowCopy()) {
                messageWrapper = serializer.readObject(requestPayload.inputBuf(), MessageWrapper.class);
            } else {
                messageWrapper = serializer.readObject(requestPayload.bytes(), MessageWrapper.class);
            }

            requestPayload.clear();
            request.setMessage(messageWrapper);
        } catch (Throwable t) {
            log.error("Failed to deserialize request from {}: {}", channel.remoteAddress(), t.getMessage(), t);
            return;
        }

        JobBeanDefinition jobBeanDefinition = processor.getJobBeanDefinition(messageWrapper.getJobBeanDefinitionName());
        if (jobBeanDefinition == null) {
            log.error("JobBeanDefinition not found: {}", messageWrapper.getJobBeanDefinitionName());
            handleException(Status.SERVICE_NOT_FOUND, new IllegalArgumentException(
                "JobBeanDefinition not found: " + messageWrapper.getJobBeanDefinitionName()));
            return;
        }

        JobContext jobContext = new JobContext(channel, request, messageWrapper, jobBeanDefinition);
        process(jobContext);

    }

    public void process(JobContext jobContext) {
        try {
            Object invokeResult = processor.invoke(jobContext);
            doProcess(invokeResult);
        }  catch (Throwable t) {
            log.error("error", t);
            handleFail(jobContext, t);
        }
    }

    private void doProcess(Object realResult) {
        ResultWrapper result = new ResultWrapper();
        result.setResult(result);
        byte s_code = request.serializerCode();
        Serializer serializer = processor.serializer(s_code);

        JResponsePayload responsePayload = new JResponsePayload(request.invokeId());

        if (CodecConfig.isCodecLowCopy()) {
            OutputBuf outputBuf =
                serializer.writeObject(channel.allocOutputBuf(), result);
            responsePayload.outputBuf(s_code, outputBuf);
        } else {
            byte[] bytes = serializer.writeObject(result);
            responsePayload.bytes(s_code, bytes);
        }

        responsePayload.status(Status.OK.value());

        handleWriteResponse(responsePayload);
    }

    private void handleWriteResponse(JResponsePayload response) {
        channel.write(response, new JFutureListener<>() {
            @Override
            public void operationSuccess(JChannel channel) {
                log.info("Response sent success");
            }

            @Override
            public void operationFailure(JChannel channel, Throwable cause) {
                long duration = SystemClock.millisClock().now() - request.timestamp();
                log.error("Response sent failed, duration: {} millis, channel: {}, cause: {}.",
                    duration, channel, cause);
            }
        });
    }

    private void handleFail(JobContext jobContext, Throwable t) {
        processor.handleException(channel, request, Status.SERVER_ERROR, t);
    }



    public void handleException(Status status, Throwable cause) {
        processor.handleException(channel, request, status, cause);
    }

    @Override
    public void rejected() {

    }
}