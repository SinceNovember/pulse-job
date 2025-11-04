package com.simple.pulsejob.admin.scheduler;

import com.simple.pulsejob.transport.JRequest;

public interface JobFilterChain {

    JobFilter getFilter();

    JobFilterChain getNext();

    <T extends JobFilterContext> void doFilter(JRequest request, T filterCtx) throws Throwable;
}
