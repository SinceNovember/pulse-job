package com.simple.pulsejob.admin.scheduler.interceptor;

import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.JResponse;
import com.simple.pulsejob.transport.channel.JChannel;

public interface JobInterceptor {

    void beforeInvoke(JRequest request, JChannel channel);

    void afterInvoke(JResponse response, JChannel channel);
}
