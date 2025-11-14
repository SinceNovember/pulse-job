package com.simple.pulsejob.admin.scheduler.filter;

import java.util.List;
import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.channel.JChannel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultScheduleFilterChains {

    private final List<ScheduleFilter> filters;

    private ScheduleFilterChain headerChain;

    public <T extends ScheduleFilterContext> void doFilter(JRequest request, JChannel channel)  throws Throwable {
        headerChain.doFilter(request, channel);
    }

    @PostConstruct
    private void compositeFilterChain() {
        ScheduleFilterChain nextChain = null;

        for (int i = filters.size() - 1; i >= 0; i--) {
            ScheduleFilter filter = filters.get(i);
            nextChain = new DefaultScheduleFilterChain(filter, nextChain);
        }

        this.headerChain = nextChain;
    }

}
