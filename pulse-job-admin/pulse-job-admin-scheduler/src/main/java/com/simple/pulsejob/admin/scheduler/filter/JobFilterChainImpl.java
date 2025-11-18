package com.simple.pulsejob.admin.scheduler.filter;

import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.channel.JChannel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JobFilterChainImpl implements JobFilterChain {

    private final JobFilter filter;

    private final JobFilterChain next;

    @Override
    public void doFilter(JRequest request, JChannel channel) throws Throwable {
        filter.doFilter(request, channel, next);
    }
}
