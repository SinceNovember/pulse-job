package com.simple.pulsejob.admin.scheduler.filter;

import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import com.simple.pulsejob.transport.JRequest;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JobFilterChainImpl implements JobFilterChain {

    private final JobFilter filter;

    private final JobFilterChain next;

    @Override
    public void doFilter(JRequest request, ScheduleContext context) throws Throwable {
        filter.doFilter(request, context, next);
    }
}
