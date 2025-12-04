package com.simple.pulsejob.admin.scheduler.filter;

import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import com.simple.pulsejob.transport.JRequest;

public interface JobFilterChain {

    void doFilter(JRequest request, ScheduleContext context) throws Throwable;
}
