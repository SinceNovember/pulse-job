package com.simple.pulsejob.admin.scheduler.interceptor;

import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.channel.JChannel;

public interface JobInterceptor {

    void beforeExecute(JRequest request, JChannel channel);

    void afterExecute(JRequest request, JChannel channel);
}
