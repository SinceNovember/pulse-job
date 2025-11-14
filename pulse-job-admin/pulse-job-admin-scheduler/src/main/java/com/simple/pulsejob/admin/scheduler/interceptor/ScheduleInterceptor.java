package com.simple.pulsejob.admin.scheduler.interceptor;

import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.JResponse;
import com.simple.pulsejob.transport.channel.JChannel;

public interface ScheduleInterceptor {

    void beforeSchedule(JRequest request, JChannel channel);

    void afterSchedule(JRequest request, JChannel channel);
}
