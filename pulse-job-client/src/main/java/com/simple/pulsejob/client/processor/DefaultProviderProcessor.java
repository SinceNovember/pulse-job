package com.simple.pulsejob.client.processor;

import com.simple.pulsejob.common.concurrent.executor.CloseableExecutor;
import com.simple.pulsejob.transport.Status;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.payload.JRequestPayload;
import com.simple.pulsejob.transport.processor.ProviderProcessor;

public class DefaultProviderProcessor implements ProviderProcessor {

    private final CloseableExecutor executor;

    public DefaultProviderProcessor() {
        this(ProviderExecutors.executor());
    }

    public DefaultProviderProcessor(CloseableExecutor executor) {
        this.executor = executor;
    }
    @Override
    public void handleRequest(JChannel channel, JRequestPayload request) throws Exception {

    }

    @Override
    public void handleException(JChannel channel, JRequestPayload request, Status status, Throwable cause) {

    }

    @Override
    public void shutdown() {

    }
}
