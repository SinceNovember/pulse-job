package com.simple.pulsejob.client.processor.task;

import com.simple.plusejob.serialization.Serializer;
import com.simple.plusejob.serialization.io.InputBuf;
import com.simple.pulsejob.client.JRequest;
import com.simple.pulsejob.client.model.metadata.MessageWrapper;
import com.simple.pulsejob.client.processor.DefaultProviderProcessor;
import com.simple.pulsejob.common.concurrent.executor.reject.RejectedRunnable;
import com.simple.pulsejob.common.util.internal.logging.InternalLogger;
import com.simple.pulsejob.common.util.internal.logging.InternalLoggerFactory;
import com.simple.pulsejob.transport.CodecConfig;
import com.simple.pulsejob.transport.Status;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.payload.JRequestPayload;

import java.util.Map;

public class MessageTask implements RejectedRunnable {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(MessageTask.class);

    private final DefaultProviderProcessor processor;
    private final JChannel channel;
    private final JRequest request;
    private final Map<Byte, Serializer> byteToSerializerMap;

    public MessageTask(DefaultProviderProcessor processor, JChannel channel, JRequest request, Map<Byte, Serializer> byteToSerializerMap) {
        this.processor = processor;
        this.channel = channel;
        this.request = request;
        this.byteToSerializerMap = byteToSerializerMap;
    }
    @Override
    public void run() {
        // stack copy
        // stack copy
        final DefaultProviderProcessor _processor = processor;
        final JRequest _request = request;

        MessageWrapper msg;
        try {
            JRequestPayload _requestPayload = _request.getPayload();

            byte s_code = _requestPayload.serializerCode();
            Serializer serializer = byteToSerializerMap.get(s_code);

            // 在业务线程中反序列化, 减轻IO线程负担
            if (CodecConfig.isCodecLowCopy()) {
                InputBuf inputBuf = _requestPayload.inputBuf();
                msg = serializer.readObject(inputBuf, MessageWrapper.class);
            } else {
                byte[] bytes = _requestPayload.bytes();
                msg = serializer.readObject(bytes, MessageWrapper.class);
            }
            _requestPayload.clear();
            _request.setMessage(msg);
        } catch (Throwable t) {
            rejected(Status.BAD_REQUEST, new JupiterBadRequestException("reading request failed", t));
            return;
        }
    }

    @Override
    public void rejected() {

    }
}
