package com.simple.pulsejob.client.interceptor;

import com.simple.pulsejob.client.JobContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 日志记录拦截器（示例）.
 *
 * <p>使用方式：将此类注册为 Spring Bean 即可自动生效</p>
 * <pre>{@code
 * @Bean
 * public JobExecutionInterceptor loggingInterceptor() {
 *     return new LoggingJobInterceptor();
 * }
 * }</pre>
 */
@Slf4j
public class LoggingJobInterceptor implements JobExecutionInterceptor {

    private static final String START_TIME_KEY = "startTime";

    @Override
    public boolean preHandle(JobContext context) {
        log.info("[Job-{}] 开始执行: handler={}", 
                context.instanceId(), context.getHandlerName());
        
        // 记录开始时间（存到 context 的扩展属性中）
        context.setAttribute(START_TIME_KEY, System.currentTimeMillis());
        return true;
    }

    @Override
    public void postHandle(JobContext context) {
        long startTime = context.getAttribute(START_TIME_KEY, 0L);
        long duration = System.currentTimeMillis() - startTime;
        
        if (context.isSuccess()) {
            log.info("[Job-{}] 执行成功: handler={}, 耗时={}ms, result={}", 
                    context.instanceId(), context.getHandlerName(), duration, context.getResult());
        } else {
            log.warn("[Job-{}] 执行失败: handler={}, 耗时={}ms", 
                    context.instanceId(), context.getHandlerName(), duration);
        }
    }

    @Override
    public void onException(JobContext context, Throwable ex) {
        log.error("[Job-{}] 执行异常: handler={}, error={}", 
                context.instanceId(), context.getHandlerName(), ex.getMessage(), ex);
    }

    @Override
    public void afterCompletion(JobContext context) {
        log.debug("[Job-{}] 执行完成（清理资源）", context.instanceId());
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE; // 最先执行
    }
}

