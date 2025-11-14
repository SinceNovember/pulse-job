package com.simple.pulsejob.admin.scheduler.filter;

import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.channel.JChannel;

public interface ScheduleFilter {

    <T extends ScheduleFilterContext> void doFilter(JRequest request, JChannel channel, ScheduleFilterChain next)
        throws Throwable;
}
