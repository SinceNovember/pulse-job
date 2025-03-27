package com.simple.pulsejob.client.processor;

import com.simple.plusejob.serialization.Serializer;
import com.simple.plusejob.serialization.SerializerType;
import com.simple.pulsejob.client.JRequest;
import com.simple.pulsejob.client.JobContext;
import com.simple.pulsejob.client.invoker.Invoker;
import com.simple.pulsejob.client.processor.task.MessageTask;
import com.simple.pulsejob.client.registry.JobBeanDefinition;
import com.simple.pulsejob.client.registry.JobBeanDefinitionLookupService;
import com.simple.pulsejob.client.registry.JobBeanDefinitionRegistry;
import com.simple.pulsejob.common.concurrent.executor.CloseableExecutor;
import com.simple.pulsejob.common.util.StackTraceUtil;
import com.simple.pulsejob.common.util.internal.logging.InternalLogger;
import com.simple.pulsejob.common.util.internal.logging.InternalLoggerFactory;
import com.simple.pulsejob.transport.Status;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.payload.JRequestPayload;
import com.simple.pulsejob.transport.processor.ClientProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DefaultClientProcessor implements ClientProcessor, JobBeanDefinitionLookupService, Invoker {

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

    @Override
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

    }
}
