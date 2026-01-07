package com.simple.pulsejob.admin.scheduler.interceptor;

import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.channel.JChannel;

public interface TransportInterceptor {

    void beforeTransport(JRequest request, JChannel channel);

    void afterTransport(JRequest request, JChannel channel);

    void onTransportFailure(JRequest request, JChannel channel, Throwable throwable);
}