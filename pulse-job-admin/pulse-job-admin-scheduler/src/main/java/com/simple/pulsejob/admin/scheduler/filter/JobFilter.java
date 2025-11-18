package com.simple.pulsejob.admin.scheduler.filter;

import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.channel.JChannel;

public interface JobFilter {

    <T> void doFilter(JRequest request, JChannel channel, JobFilterChain next)
        throws Throwable;
}
