package com.simple.pulsejob.admin.scheduler.processor.task;

import com.simple.pulsejob.transport.JResponse;
import com.simple.pulsejob.transport.channel.JChannel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MessageTask implements Runnable {

    private final JChannel channel;
    private final JResponse response;

    @Override
    public void run() {

    }
}
