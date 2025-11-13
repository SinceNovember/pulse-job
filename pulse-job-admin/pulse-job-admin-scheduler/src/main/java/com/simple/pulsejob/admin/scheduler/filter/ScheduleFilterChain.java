package com.simple.pulsejob.admin.scheduler.filter;

import com.simple.pulsejob.transport.JRequest;

public interface ScheduleFilterChain {

    ScheduleFilter getFilter();

    ScheduleFilterChain getNext();

    <T extends ScheduleFilterContext> void doFilter(JRequest request, T filterCtx) throws Throwable;
}
