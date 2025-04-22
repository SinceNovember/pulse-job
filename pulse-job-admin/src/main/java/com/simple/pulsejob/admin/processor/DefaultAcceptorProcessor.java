package com.simple.pulsejob.admin.processor;

import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.payload.JResponsePayload;
import com.simple.pulsejob.transport.processor.AcceptorProcessor;

public class DefaultAcceptorProcessor implements AcceptorProcessor {
    @Override
    public void handleResponse(JChannel channel, JResponsePayload response) throws Exception {


    }

    @Override
    public void shutdown() {

    }
}
