package com.simple.pulsejob.admin.scheduler;

import com.simple.pulsejob.transport.JRequest;

public interface JFilterChain {

    JFilter getFilter();

    JFilterChain getNext();

    <T extends JFilterContext> void doFilter(JRequest request, T filterCtx) throws Throwable;
}
