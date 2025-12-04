package com.simple.pulsejob.admin.scheduler.filter;

import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import com.simple.pulsejob.transport.JRequest;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JobFilterChains {

    private final List<JobFilter> filters;

    private JobFilterChain headerChain;

    public void doFilter(JRequest request, ScheduleContext context)  throws Throwable {
        headerChain.doFilter(request, context);
    }

    @PostConstruct
    private void compositeFilterChain() {
        JobFilterChain nextChain = null;

        for (int i = filters.size() - 1; i >= 0; i--) {
            JobFilter filter = filters.get(i);
            nextChain = new JobFilterChainImpl(filter, nextChain);
        }

        this.headerChain = nextChain;
    }

}
