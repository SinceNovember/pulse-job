package com.simple.pulsejob.client.interceptor;

import com.simple.pulsejob.client.JobContext;

/**
 * 任务执行拦截器.
 *
 * <p>在任务执行前后进行拦截处理，可用于：</p>
 * <ul>
 *   <li>日志记录</li>
 *   <li>性能监控</li>
 *   <li>上下文准备/清理</li>
 *   <li>异常处理</li>
 *   <li>权限校验</li>
 * </ul>
 *
 * <p>执行顺序：</p>
 * <pre>
 * interceptor1.preHandle()
 *   interceptor2.preHandle()
 *     ... 执行任务 ...
 *   interceptor2.postHandle()
 * interceptor1.postHandle()
 * </pre>
 */
public interface JobExecutionInterceptor {

    /**
     * 任务执行前调用
     *
     * @param context 任务上下文
     * @return true 继续执行，false 中断执行
     */
    default boolean preHandle(JobContext context) {
        return true;
    }

    /**
     * 任务执行后调用（无论成功或失败）
     *
     * @param context 任务上下文（包含执行结果或异常）
     */
    default void postHandle(JobContext context) {
    }

    /**
     * 任务执行异常时调用
     *
     * @param context 任务上下文
     * @param ex      异常
     */
    default void onException(JobContext context, Throwable ex) {
    }

    /**
     * 任务执行完成后调用（在 postHandle 之后，无论成功失败都会调用）
     *
     * <p>适合做资源清理工作</p>
     *
     * @param context 任务上下文
     */
    default void afterCompletion(JobContext context) {
    }

    /**
     * 拦截器顺序，数值越小越先执行
     */
    default int getOrder() {
        return 0;
    }
}

