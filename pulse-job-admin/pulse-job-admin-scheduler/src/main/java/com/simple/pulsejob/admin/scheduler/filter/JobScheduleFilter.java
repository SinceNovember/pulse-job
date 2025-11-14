package com.simple.pulsejob.admin.scheduler.filter;

import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.channel.JFutureListener;
import com.simple.pulsejob.transport.payload.JRequestPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Order(99)
@Component
@RequiredArgsConstructor
public class JobScheduleFilter implements ScheduleFilter {

    @Override
    public <T extends ScheduleFilterContext> void doFilter(JRequest request, JChannel channel, ScheduleFilterChain next)
        throws Throwable {
        final JRequestPayload payload = request.payload();
        channel.write(payload, new JFutureListener<>() {
            @Override
            public void operationSuccess(JChannel channel) throws Exception {
            }

            @Override
            public void operationFailure(JChannel channel, Throwable cause) throws Exception {
            }
        });

    }
}
