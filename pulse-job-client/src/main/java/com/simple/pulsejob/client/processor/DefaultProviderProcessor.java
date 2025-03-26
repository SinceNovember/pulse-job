package com.simple.pulsejob.client.processor;

import com.simple.plusejob.serialization.Serializer;
import com.simple.pulsejob.client.JRequest;
import com.simple.pulsejob.client.processor.task.MessageTask;
import com.simple.pulsejob.common.concurrent.executor.CloseableExecutor;
import com.simple.pulsejob.transport.Status;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.payload.JRequestPayload;
import com.simple.pulsejob.transport.processor.ProviderProcessor;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class DefaultProviderProcessor implements ProviderProcessor {

    private final CloseableExecutor executor;

    private final List<Serializer> serializers;

    @Override
    public void handleRequest(JChannel channel, JRequestPayload requestPayload) throws Exception {
        MessageTask task = new MessageTask(
                this, channel, new JRequest(requestPayload), convertSerializersToByteMap(serializers));
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

    private Map<Byte, Serializer> convertSerializersToByteMap(List<Serializer> serializers) {
        return serializers.stream().collect(
                Collectors.toMap(Serializer::code, Function.identity()));
    }

}
