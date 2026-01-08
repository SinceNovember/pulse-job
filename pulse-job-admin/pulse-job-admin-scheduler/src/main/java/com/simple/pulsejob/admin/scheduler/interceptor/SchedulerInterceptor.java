package com.simple.pulsejob.admin.scheduler.interceptor;

import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.JResponse;
import com.simple.pulsejob.transport.channel.JChannel;

public interface SchedulerInterceptor {

    void beforeSchedule(ScheduleContext context, JChannel channel);

    void beforeTransport(ScheduleContext context, JChannel channel);

    void afterTransport(ScheduleContext context, JChannel channel, JRequest request);

    void onTransportFailure(ScheduleContext context, JChannel channel, JRequest request, Throwable throwable);

    void afterSchedule(ScheduleContext context,  JChannel channel, JResponse response);

    void onScheduleFailure(ScheduleContext context, JChannel channel, JRequest request);

}