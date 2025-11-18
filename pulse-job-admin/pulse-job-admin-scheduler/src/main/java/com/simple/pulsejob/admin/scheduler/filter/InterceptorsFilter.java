package com.simple.pulsejob.admin.scheduler.filter;

import java.util.List;
import com.simple.pulsejob.admin.scheduler.interceptor.JobInterceptor;
import com.simple.pulsejob.common.util.Reflects;
import com.simple.pulsejob.common.util.StackTraceUtil;
import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.channel.JChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Slf4j
@Order(98)
@Component
@RequiredArgsConstructor
public class InterceptorsFilter implements JobFilter {

    private final List<JobInterceptor> interceptors;

    @Override
    public <T> void doFilter(JRequest request, JChannel channel, JobFilterChain next)
        throws Throwable {
        JobFilterContext jobFilterContext = (JobFilterContext) channel;
        if (CollectionUtils.isEmpty(interceptors)) {
            next.doFilter(request, channel);
        }

        handleBeforeSchedule(request, jobFilterContext);
        try {
            if (next != null) {
                next.doFilter(request, channel);
            }
        } finally {
            handleAfterSchedule(request, jobFilterContext);
        }
    }

    private void handleBeforeSchedule(JRequest request, JobFilterContext jobFilterContext) {
        for (JobInterceptor interceptor : interceptors) {
            try {
                interceptor.beforeExecute(request, jobFilterContext.getChannel());
            } catch (Throwable t) {
                log.error("Interceptor[{}#afterInvoke]: {}.", Reflects.simpleClassName(interceptor),
                    StackTraceUtil.stackTrace(t));
            }
        }
    }

    private void handleAfterSchedule(JRequest request, JobFilterContext jobFilterContext) {
        for (int i = interceptors.size() - 1; i >= 0; i--) {
            try {
                interceptors.get(i).afterExecute(request, jobFilterContext.getChannel());
            } catch (Throwable t) {
                log.error("Interceptor[{}#afterInvoke]: {}.", Reflects.simpleClassName(interceptors.get(i)),
                    StackTraceUtil.stackTrace(t));
            }
        }
    }
}
