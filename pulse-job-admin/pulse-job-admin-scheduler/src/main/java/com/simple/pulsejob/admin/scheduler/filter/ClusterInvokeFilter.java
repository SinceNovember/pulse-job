package com.simple.pulsejob.admin.scheduler.filter;

import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import com.simple.pulsejob.transport.JRequest;
import org.springframework.stereotype.Component;

@Component
public class ClusterInvokeFilter implements JobFilter {
    @Override
    public void doFilter(JRequest request, ScheduleContext context, JobFilterChain next) throws Throwable {
        // 预留扩展点
        if (next != null) {
            next.doFilter(request, context);
        }
    }
}
