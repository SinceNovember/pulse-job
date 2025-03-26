package com.simple.pulsejob.client.processor.task;

import com.simple.plusejob.serialization.Serializer;
import com.simple.plusejob.serialization.SerializerFactory;
import com.simple.pulsejob.client.JRequest;
import com.simple.pulsejob.client.processor.DefaultProviderProcessor;
import com.simple.pulsejob.common.concurrent.executor.reject.RejectedRunnable;
import com.simple.pulsejob.common.util.internal.logging.InternalLogger;
import com.simple.pulsejob.common.util.internal.logging.InternalLoggerFactory;
import com.simple.pulsejob.transport.Status;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.payload.JRequestPayload;

public class MessageTask implements RejectedRunnable {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MessageTask.class);

    private final JChannel channel;
    private final JRequest request;

    public MessageTask(JChannel channel, JRequest request) {
        this.channel = channel;
        this.request = request;
    }

    @Override
    public void run() {
        // stack copy
        // stack copy
        final DefaultProviderProcessor _processor = processor;
        final JRequest _request = request;
        try {
            JRequestPayload _requestPayload = _request.payload();

            byte s_code = _requestPayload.serializerCode();
            Serializer serializer = SerializerFactory.getSerializer(s_code);

        } catch (Throwable t) {
            rejected(Status.BAD_REQUEST, new JupiterBadRequestException("reading request failed", t));
            return;
        }
    }

    @Override
    public void rejected() {

    }
}
