package com.simple.pulsejob.client.processor;

import com.simple.plusejob.serialization.Serializer;
import com.simple.plusejob.serialization.io.OutputBuf;
import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.client.JobContext;
import com.simple.pulsejob.client.invoker.Invoker;
import com.simple.pulsejob.transport.metadata.ResultWrapper;
import com.simple.pulsejob.client.processor.task.MessageTask;
import com.simple.pulsejob.client.registry.JobBeanDefinition;
import com.simple.pulsejob.client.registry.JobBeanDefinitionLookupService;
import com.simple.pulsejob.client.registry.JobBeanDefinitionRegistry;
import com.simple.pulsejob.common.concurrent.executor.CloseableExecutor;
import com.simple.pulsejob.common.util.StackTraceUtil;
import com.simple.pulsejob.common.util.ThrowUtil;
import com.simple.pulsejob.common.util.internal.logging.InternalLogger;
import com.simple.pulsejob.common.util.internal.logging.InternalLoggerFactory;
import com.simple.pulsejob.transport.CodecConfig;
import com.simple.pulsejob.transport.Status;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.channel.JFutureListener;
import com.simple.pulsejob.transport.payload.JRequestPayload;
import com.simple.pulsejob.transport.payload.JResponsePayload;
import com.simple.pulsejob.transport.processor.ConnectorProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class DefaultClientProcessor implements ConnectorProcessor, JobBeanDefinitionLookupService {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultClientProcessor.class);

    private final CloseableExecutor executor;

    private final Map<Byte, Serializer> serializerMap;

    private final JobBeanDefinitionRegistry jobBeanDefinitionRegistry;

    private final Invoker invoker;

    @Override
    public Serializer serializer(Byte code) {
        return serializerMap.get(code);
    }

    @Override
    public void handleRequest(JChannel channel, JRequestPayload requestPayload) throws Exception {
        MessageTask task = new MessageTask(
            this, channel, new JRequest(requestPayload));
        if (executor == null) {
            channel.addTask(task);
        } else {
            executor.execute(task);
        }
    }

    @Override
    public void handleException(JChannel channel, JRequestPayload request, Status status, Throwable cause) {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public JobBeanDefinition getJobBeanDefinition(String jobBeanDefinitionName) {
        return jobBeanDefinitionRegistry.getJobBeanDefinition(jobBeanDefinitionName);
    }

    public Object invoke(JobContext jobContext) {
        return invoker.invoke(jobContext);
    }

    public void handleException(JChannel channel, JRequest request, Status status, Throwable cause) {
        logger.error("An exception was caught while processing request: {}, {}.",
            channel.remoteAddress(), StackTraceUtil.stackTrace(cause));

        doHandleException(
            channel, request.invokeId(), request.serializerCode(), status.value(), cause, false);
    }

    private void doHandleException(
        JChannel channel, long invokeId, byte s_code, byte status, Throwable cause, boolean closeChannel) {

        ResultWrapper result = new ResultWrapper();
        // 截断cause, 避免客户端无法找到cause类型而无法序列化
        result.setError(ThrowUtil.cutCause(cause));

        Serializer serializer = serializer(s_code);

        JResponsePayload response = new JResponsePayload(invokeId);
        response.status(status);
        if (CodecConfig.isCodecLowCopy()) {
            OutputBuf outputBuf =
                serializer.writeObject(channel.allocOutputBuf(), result);
            response.outputBuf(s_code, outputBuf);
        } else {
            byte[] bytes = serializer.writeObject(result);
            response.bytes(s_code, bytes);
        }

        if (closeChannel) {
            channel.write(response, JChannel.CLOSE);
        } else {
            channel.write(response, new JFutureListener<>() {

                @Override
                public void operationSuccess(JChannel channel) throws Exception {
                    logger.debug("Service error message sent out: {}.", channel);
                }

                @Override
                public void operationFailure(JChannel channel, Throwable cause) throws Exception {
                    if (logger.isWarnEnabled()) {
                        logger.warn("Service error message sent failed: {}, {}.", channel,
                            StackTraceUtil.stackTrace(cause));
                    }
                }
            });
        }
    }
}
