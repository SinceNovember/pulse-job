package com.simple.pulsejob.admin.scheduler.filter;

import java.util.List;
import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.channel.JChannel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobFilterChains {

    private final List<JobFilter> filters;

    private JobFilterChain headerChain;

    public void doFilter(JRequest request, JChannel channel)  throws Throwable {
        headerChain.doFilter(request, channel);
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
