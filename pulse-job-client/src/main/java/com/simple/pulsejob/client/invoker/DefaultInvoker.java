package com.simple.pulsejob.client.invoker;

import com.simple.pulsejob.client.JobContext;
import com.simple.pulsejob.common.util.Reflects;
import org.slf4j.MDC;

public class DefaultInvoker implements Invoker {

    @Override
    public Object invoke(JobContext jobContext) {
        try {
            MDC.put("invokeId", String.valueOf(jobContext.invokeId()));
            return Reflects.fastInvoke(jobContext.targetBean(), jobContext.targetMethodName(),
                jobContext.parameterTypes(), null);
        } catch (Throwable t) {
            jobContext.setCause(t);
        } finally {
            MDC.remove("invokeId");
        }
        return null;
    }
}
