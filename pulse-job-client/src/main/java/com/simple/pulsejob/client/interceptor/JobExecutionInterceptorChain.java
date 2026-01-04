package com.simple.pulsejob.client.interceptor;

import com.simple.pulsejob.client.JobContext;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 拦截器链.
 *
 * <p>管理多个拦截器的执行顺序，确保正确的调用流程</p>
 */
@Slf4j
public class JobExecutionInterceptorChain {

    private final List<JobExecutionInterceptor> interceptors;

    public JobExecutionInterceptorChain(List<JobExecutionInterceptor> interceptors) {
        this.interceptors = interceptors != null
                ? sortByOrder(interceptors)
                : new ArrayList<>();
    }

    /**
     * 执行前置拦截
     *
     * @return 返回最后一个成功执行的拦截器索引，-1 表示全部成功
     */
    public int applyPreHandle(JobContext context) {
        for (int i = 0; i < interceptors.size(); i++) {
            JobExecutionInterceptor interceptor = interceptors.get(i);
            try {
                if (!interceptor.preHandle(context)) {
                    // 拦截器返回 false，触发已执行拦截器的 afterCompletion
                    triggerAfterCompletion(context, i - 1);
                    return i;
                }
            } catch (Exception e) {
                log.error("拦截器 preHandle 异常: {}", interceptor.getClass().getName(), e);
                triggerAfterCompletion(context, i - 1);
                throw e;
            }
        }
        return -1; // 全部成功
    }

    /**
     * 执行后置拦截（逆序执行）
     */
    public void applyPostHandle(JobContext context) {
        for (int i = interceptors.size() - 1; i >= 0; i--) {
            JobExecutionInterceptor interceptor = interceptors.get(i);
            try {
                interceptor.postHandle(context);
            } catch (Exception e) {
                log.error("拦截器 postHandle 异常: {}", interceptor.getClass().getName(), e);
            }
        }
    }

    /**
     * 执行异常处理（逆序执行）
     */
    public void applyOnException(JobContext context, Throwable ex) {
        for (int i = interceptors.size() - 1; i >= 0; i--) {
            JobExecutionInterceptor interceptor = interceptors.get(i);
            try {
                interceptor.onException(context, ex);
            } catch (Exception e) {
                log.error("拦截器 onException 异常: {}", interceptor.getClass().getName(), e);
            }
        }
    }

    /**
     * 触发 afterCompletion（从指定索引逆序执行）
     */
    public void triggerAfterCompletion(JobContext context, int interceptorIndex) {
        for (int i = interceptorIndex; i >= 0; i--) {
            JobExecutionInterceptor interceptor = interceptors.get(i);
            try {
                interceptor.afterCompletion(context);
            } catch (Exception e) {
                log.error("拦截器 afterCompletion 异常: {}", interceptor.getClass().getName(), e);
            }
        }
    }

    /**
     * 触发所有拦截器的 afterCompletion
     */
    public void triggerAfterCompletion(JobContext context) {
        triggerAfterCompletion(context, interceptors.size() - 1);
    }

    public boolean isEmpty() {
        return interceptors.isEmpty();
    }

    public int size() {
        return interceptors.size();
    }

    private List<JobExecutionInterceptor> sortByOrder(List<JobExecutionInterceptor> list) {
        List<JobExecutionInterceptor> sorted = new ArrayList<>(list);
        sorted.sort(Comparator.comparingInt(JobExecutionInterceptor::getOrder));
        return sorted;
    }
}

