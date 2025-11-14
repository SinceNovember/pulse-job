package com.simple.pulsejob.admin.scheduler.filter;

import java.util.List;
import com.simple.pulsejob.admin.scheduler.interceptor.ScheduleInterceptor;
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
public class InterceptorsFilter implements ScheduleFilter {

    private final List<ScheduleInterceptor> interceptors;

    @Override
    public <T extends ScheduleFilterContext> void doFilter(JRequest request, JChannel channel, ScheduleFilterChain next)
        throws Throwable {
        ScheduleContext scheduleContext = (ScheduleContext) channel;
        if (CollectionUtils.isEmpty(interceptors)) {
            next.doFilter(request, channel);
        }

        handleBeforeSchedule(request, scheduleContext);
        try {
            if (next != null) {
                next.doFilter(request, channel);
            }
        } finally {
            handleAfterSchedule(request, scheduleContext);
        }

    }

    private void handleBeforeSchedule(JRequest request, ScheduleContext scheduleContext) {
        for (ScheduleInterceptor interceptor : interceptors) {
            try {
                interceptor.beforeSchedule(request, scheduleContext.getChannel());
            } catch (Throwable t) {
                log.error("Interceptor[{}#afterInvoke]: {}.", Reflects.simpleClassName(interceptor),
                    StackTraceUtil.stackTrace(t));
            }
        }
    }

    private void handleAfterSchedule(JRequest request, ScheduleContext scheduleContext) {
        for (int i = interceptors.size() - 1; i >= 0; i--) {
            try {
                interceptors.get(i).afterSchedule(request, scheduleContext.getChannel());
            } catch (Throwable t) {
                log.error("Interceptor[{}#afterInvoke]: {}.", Reflects.simpleClassName(interceptors.get(i)),
                    StackTraceUtil.stackTrace(t));
            }
        }
    }
}
