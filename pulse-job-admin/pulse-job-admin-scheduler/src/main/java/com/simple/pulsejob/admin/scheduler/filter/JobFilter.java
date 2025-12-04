package com.simple.pulsejob.admin.scheduler.filter;

import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import com.simple.pulsejob.transport.JRequest;

public interface JobFilter {

    void doFilter(JRequest request, ScheduleContext context, JobFilterChain next)
            throws Throwable;
}
