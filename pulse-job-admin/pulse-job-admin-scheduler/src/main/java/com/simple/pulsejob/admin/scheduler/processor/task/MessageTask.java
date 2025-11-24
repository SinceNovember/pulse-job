package com.simple.pulsejob.admin.scheduler.processor.task;

import com.simple.plusejob.serialization.Serializer;
import com.simple.plusejob.serialization.io.InputBuf;
import com.simple.pulsejob.admin.scheduler.factory.SerializerFactory;
import com.simple.pulsejob.admin.scheduler.future.DefaultInvokeFuture;
import com.simple.pulsejob.common.util.StackTraceUtil;
import com.simple.pulsejob.transport.JResponse;
import com.simple.pulsejob.transport.Status;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.metadata.ResultWrapper;
import com.simple.pulsejob.transport.payload.JResponsePayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MessageTask implements Runnable {

    private final JChannel channel;
    private final JResponse response;

    @Override
    public void run() {
        // stack copy
        final JResponse _response = response;
        final JResponsePayload _responsePayload = _response.payload();

        byte s_code = _response.serializerCode();

        Serializer serializer = SerializerFactory.getSerializer(s_code);
        ResultWrapper wrapper;
        try {
                InputBuf inputBuf = _responsePayload.inputBuf();
                wrapper = serializer.readObject(inputBuf, ResultWrapper.class);

            _responsePayload.clear();
        } catch (Throwable t) {
            log.error("Deserialize object failed: {}, {}.", channel.remoteAddress(), StackTraceUtil.stackTrace(t));

            _response.status(Status.DESERIALIZATION_FAIL);
            wrapper = new ResultWrapper();
            wrapper.setError(new RuntimeException(t));
        }
        _response.result(wrapper);

        DefaultInvokeFuture.received(channel, _response);
    }
}
