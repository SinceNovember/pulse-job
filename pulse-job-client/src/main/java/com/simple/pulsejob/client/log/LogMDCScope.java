package com.simple.pulsejob.client.log;

import com.simple.pulsejob.client.JobContext;
import com.simple.pulsejob.client.context.JobContextHolder;

/**
 * 日志上下文作用域（自动管理生命周期）.
 *
 * <p>使用 try-with-resources 自动设置和清理任务上下文：</p>
 * <pre>
 * try (LogMDCScope scope = LogMDCScope.open(jobContext)) {
 *     // 这里的日志会自动关联到任务
 *     log.info("执行任务...");
 * }
 * </pre>
 */
public final class LogMDCScope implements AutoCloseable {

    public static final String INSTANCE_ID = "instanceId";

    /**
     * 开启日志上下文作用域.
     *
     * @param ctx 任务上下文
     * @return LogMDCScope 实例
     */
    public static LogMDCScope open(JobContext ctx) {
        JobContextHolder.set(ctx);
        return new LogMDCScope();
    }

    @Override
    public void close() {
        JobContextHolder.clear();
    }
}