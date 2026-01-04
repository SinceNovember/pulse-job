package com.simple.pulsejob.client.invoker;

import java.lang.reflect.Method;

import com.simple.pulsejob.client.JobContext;
import com.simple.pulsejob.client.interceptor.JobExecutionInterceptorChain;
import com.simple.pulsejob.client.log.LogMDCScope;
import com.simple.pulsejob.client.registry.JobHandlerHolder;
import com.simple.pulsejob.client.registry.JobHandlerRegistry;
import com.simple.pulsejob.common.util.Reflects;
import lombok.extern.slf4j.Slf4j;

/**
 * 默认任务调用器.
 *
 * <p>支持拦截器链，执行流程：</p>
 * <pre>
 * 1. preHandle (拦截器链正序)
 * 2. invoke (任务执行)
 * 3. postHandle (拦截器链逆序)
 * 4. afterCompletion (拦截器链逆序，无论成功失败)
 * </pre>
 */
@Slf4j
public class DefaultInvoker implements Invoker {

    private final JobHandlerRegistry jobRegistry;
    private final JobExecutionInterceptorChain interceptorChain;

    public DefaultInvoker(JobHandlerRegistry jobRegistry) {
        this(jobRegistry, null);
    }

    public DefaultInvoker(JobHandlerRegistry jobRegistry, JobExecutionInterceptorChain interceptorChain) {
        this.jobRegistry = jobRegistry;
        this.interceptorChain = interceptorChain;
    }

    @Override
    public Object invoke(JobContext jobContext) {
        try (LogMDCScope ignored = LogMDCScope.open(jobContext)) {
            String handlerName = jobContext.getHandlerName();
            JobHandlerHolder handlerHolder = jobRegistry.getJobHandlerHolder(handlerName);
            if (handlerHolder == null) {
                throw new IllegalArgumentException("No job registered for name: " + handlerName);
            }

            // 无拦截器时直接执行
            if (interceptorChain == null || interceptorChain.isEmpty()) {
                return doInvoke(jobContext, handlerHolder);
            }

            // 有拦截器时走拦截器链
            return invokeWithInterceptors(jobContext, handlerHolder);

        } catch (Throwable t) {
            jobContext.setCause(t);
            throw t instanceof RuntimeException ? (RuntimeException) t : new RuntimeException(t);
        }
    }

    /**
     * 带拦截器的执行流程
     */
    private Object invokeWithInterceptors(JobContext jobContext, JobHandlerHolder handlerHolder) {
        Object result = null;
        Throwable ex = null;

        try {
            // 1. 前置拦截
            int interceptorIndex = interceptorChain.applyPreHandle(jobContext);
            if (interceptorIndex >= 0) {
                // 被拦截器中断
                log.debug("任务被拦截器中断: handler={}, interceptorIndex={}",
                        jobContext.getHandlerName(), interceptorIndex);
                return null;
            }

            // 2. 执行任务
            result = doInvoke(jobContext, handlerHolder);
            jobContext.setResult(result);

            // 3. 后置拦截
            interceptorChain.applyPostHandle(jobContext);

            return result;

        } catch (Throwable t) {
            ex = t;
            jobContext.setCause(t);

            // 异常处理
            interceptorChain.applyOnException(jobContext, t);

            throw t instanceof RuntimeException ? (RuntimeException) t : new RuntimeException(t);

        } finally {
            // 4. 完成回调（无论成功失败都执行）
            interceptorChain.triggerAfterCompletion(jobContext);
        }
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
