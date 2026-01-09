package com.simple.pulsejob.client.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.simple.pulsejob.client.JobContext;

/**
 * 任务上下文持有者（基于 TTL，支持异步传递）.
 *
 * <p>在任务执行期间持有 {@link JobContext}，支持跨线程传递。</p>
 *
 * <p>如果项目有异步场景（线程池），推荐使用 TTL Agent：</p>
 * <pre>
 * java -javaagent:transmittable-thread-local-2.14.5.jar -jar app.jar
 * </pre>
 *
 * @see JobContext
 */
public final class JobContextHolder {

    private static final TransmittableThreadLocal<JobContext> CONTEXT = new TransmittableThreadLocal<>();

    private JobContextHolder() {}

    /**
     * 设置当前任务上下文.
     *
     * @param context 任务上下文
     */
    public static void set(JobContext context) {
        CONTEXT.set(context);
    }

    /**
     * 获取当前任务上下文.
     *
     * @return 任务上下文，无上下文时返回 null
     */
    public static JobContext get() {
        return CONTEXT.get();
    }

    /**
     * 清除当前任务上下文.
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 获取当前实例ID.
     *
     * @return 实例ID，无上下文时返回 null
     */
    public static Long getInstanceId() {
        JobContext ctx = CONTEXT.get();
        return ctx != null ? ctx.instanceId() : null;
    }

    /**
     * 获取当前任务ID.
     *
     * @return 任务ID，无上下文时返回 null
     */
    public static Integer getJobId() {
        JobContext ctx = CONTEXT.get();
        return ctx != null ? ctx.getJobId() : null;
    }

    /**
     * 判断当前是否在任务上下文中.
     *
     * @return 是否有任务上下文
     */
    public static boolean hasContext() {
        return CONTEXT.get() != null;
    }
}

