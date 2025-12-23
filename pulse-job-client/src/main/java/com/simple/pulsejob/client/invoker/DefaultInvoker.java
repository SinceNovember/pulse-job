package com.simple.pulsejob.client.invoker;

import java.lang.reflect.Method;
import com.simple.pulsejob.client.JobContext;
import com.simple.pulsejob.client.log.LogMDCScope;
import com.simple.pulsejob.client.registry.JobHandlerHolder;
import com.simple.pulsejob.client.registry.JobHandlerRegistry;
import com.simple.pulsejob.common.util.Reflects;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;

@RequiredArgsConstructor
public class DefaultInvoker implements Invoker {

    private final JobHandlerRegistry jobRegistry;

    @Override
    public Object invoke(JobContext jobContext) {
        try (LogMDCScope ignored = LogMDCScope.open(jobContext)){
            String handlerName = jobContext.getHandlerName();
            JobHandlerHolder handlerHolder = jobRegistry.getJobHandlerHolder(handlerName);
            if (handlerHolder == null) {
                throw new IllegalArgumentException("No job registered for name: " + handlerName);
            }
            return doInvoke(jobContext, handlerHolder);

        } catch (Throwable t) {
            jobContext.setCause(t);
        }
        return null;
    }

    private Object doInvoke(JobContext jobContext, JobHandlerHolder handlerHolder) {
        Object bean = handlerHolder.getBean();
        Method method = handlerHolder.getMethod();
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] args = jobContext.getArgs();
        if (args == null) {
            args = new Object[0];
        }
        return Reflects.fastInvoke(bean, method.getName(), parameterTypes, args);
    }
}
