package com.simple.pulsejob.client.invoker;

import com.simple.pulsejob.client.JobContext;
import com.simple.pulsejob.common.util.Reflects;
import org.slf4j.MDC;

public class DefaultInvoker implements Invoker {

    @Override
    public Object invoke(JobContext jobContext) {
        MDC.put("invokeId", String.valueOf(jobContext.invokeId()));
        try {
            Object o = Reflects.fastInvoke(jobContext.targetBean(), jobContext.targetMethodName(),
                jobContext.parameterTypes(), jobContext.args());
        } catch (Throwable t) {
        }
        return null;

    }
}
