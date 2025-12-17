package com.simple.pulsejob.client.invoker;

import com.simple.pulsejob.client.JobContext;
import com.simple.pulsejob.client.registry.JobRegistry;
import com.simple.pulsejob.common.util.Reflects;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
public class DefaultInvoker implements Invoker {

    private final JobRegistry jobRegistry;

    @Override
    public Object invoke(JobContext jobContext) {
        try {
            MDC.put("invokeId", String.valueOf(jobContext.invokeId()));
            return jobRegistry.invoke(jobContext.getHandlerName(), jobContext.getArgs());
        } catch (Throwable t) {
            jobContext.setCause(t);
        } finally {
            MDC.remove("invokeId");
        }
        return null;
    }
}
