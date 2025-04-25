package com.simple.pulsejob.admin.processor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.channel.JChannelGroup;
import com.simple.pulsejob.transport.netty.channel.NettyChannelGroup;
import com.simple.pulsejob.transport.payload.JResponsePayload;
import com.simple.pulsejob.transport.processor.AcceptorProcessor;

public class DefaultAcceptorProcessor implements AcceptorProcessor {
    public static final List<JChannel> CLIENT_CHANNELS = new ArrayList<>();

    @Override
    public void handleResponse(JChannel channel, JResponsePayload response) throws Exception {
    }

    @Override
    public void handleActive(JChannel channel) {
        CLIENT_CHANNELS.add(channel);
    }

    @Override
    public void shutdown() {

    }
}
