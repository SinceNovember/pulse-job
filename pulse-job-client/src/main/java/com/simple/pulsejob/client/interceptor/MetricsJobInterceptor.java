package com.simple.pulsejob.client.interceptor;

import com.simple.pulsejob.client.JobContext;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * 指标统计拦截器（示例）.
 *
 * <p>统计任务执行次数、成功/失败次数、平均耗时等</p>
 *
 * <p>使用方式：</p>
 * <pre>{@code
 * @Bean
 * public JobExecutionInterceptor metricsInterceptor() {
 *     return new MetricsJobInterceptor();
 * }
 * }</pre>
 */
@Slf4j
public class MetricsJobInterceptor implements JobExecutionInterceptor {

    private static final String START_TIME_KEY = "_metrics_startTime";

    /** 总执行次数 */
    private final LongAdder totalCount = new LongAdder();

    /** 成功次数 */
    private final LongAdder successCount = new LongAdder();

    /** 失败次数 */
    private final LongAdder failureCount = new LongAdder();

    /** 总耗时（用于计算平均值） */
    private final LongAdder totalDuration = new LongAdder();

    /** 按 handler 分组的统计 */
    private final ConcurrentMap<String, HandlerMetrics> handlerMetrics = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(JobContext context) {
        context.setAttribute(START_TIME_KEY, System.currentTimeMillis());
        totalCount.increment();
        getOrCreateMetrics(context.getHandlerName()).totalCount.increment();
        return true;
    }

    @Override
    public void postHandle(JobContext context) {
        long startTime = context.getAttribute(START_TIME_KEY, 0L);
        long duration = System.currentTimeMillis() - startTime;

        totalDuration.add(duration);
        HandlerMetrics metrics = getOrCreateMetrics(context.getHandlerName());
        metrics.totalDuration.add(duration);

        if (context.isSuccess()) {
            successCount.increment();
            metrics.successCount.increment();
        } else {
            failureCount.increment();
            metrics.failureCount.increment();
        }
    }

    @Override
    public void onException(JobContext context, Throwable ex) {
        // 异常已在 postHandle 中通过 isSuccess() 处理
    }

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE + 1; // 在日志拦截器之后
    }

    // ==================== 查询方法 ====================

    public long getTotalCount() {
        return totalCount.sum();
    }

    public long getSuccessCount() {
        return successCount.sum();
    }

    public long getFailureCount() {
        return failureCount.sum();
    }

    public double getAverageDuration() {
        long count = totalCount.sum();
        return count > 0 ? (double) totalDuration.sum() / count : 0;
    }

    public double getSuccessRate() {
        long total = totalCount.sum();
        return total > 0 ? (double) successCount.sum() / total * 100 : 0;
    }

    public HandlerMetrics getHandlerMetrics(String handlerName) {
        return handlerMetrics.get(handlerName);
    }

    private HandlerMetrics getOrCreateMetrics(String handlerName) {
        return handlerMetrics.computeIfAbsent(handlerName, k -> new HandlerMetrics());
    }

    /**
     * 单个 Handler 的统计数据
     */
    public static class HandlerMetrics {
        public final LongAdder totalCount = new LongAdder();
        public final LongAdder successCount = new LongAdder();
        public final LongAdder failureCount = new LongAdder();
        public final LongAdder totalDuration = new LongAdder();

        public double getAverageDuration() {
            long count = totalCount.sum();
            return count > 0 ? (double) totalDuration.sum() / count : 0;
        }

        public double getSuccessRate() {
            long total = totalCount.sum();
            return total > 0 ? (double) successCount.sum() / total * 100 : 0;
        }

        @Override
        public String toString() {
            return String.format("total=%d, success=%d, failure=%d, avgDuration=%.2fms, successRate=%.2f%%",
                    totalCount.sum(), successCount.sum(), failureCount.sum(),
                    getAverageDuration(), getSuccessRate());
        }
    }

    @Override
    public String toString() {
        return String.format("MetricsJobInterceptor{total=%d, success=%d, failure=%d, avgDuration=%.2fms, successRate=%.2f%%}",
                getTotalCount(), getSuccessCount(), getFailureCount(),
                getAverageDuration(), getSuccessRate());
    }
}

