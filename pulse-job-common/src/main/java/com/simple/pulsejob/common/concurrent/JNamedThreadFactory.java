package com.simple.pulsejob.common.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import com.simple.pulsejob.common.util.Requires;
import com.simple.pulsejob.common.util.internal.InternalThread;

public class JNamedThreadFactory implements ThreadFactory {

    private static final String DEFAULT_THREAD_NAME_PREFIX = "pulse-job-";
    private static final String DEFAULT_THREAD_POOL_NAME = "default";
    private final AtomicInteger id = new AtomicInteger();
    private final String name;
    private final boolean daemon;
    private final int priority;
    private final ThreadGroup group;

    public JNamedThreadFactory() {
        this(DEFAULT_THREAD_POOL_NAME);
    }

    public JNamedThreadFactory(String name) {
        this(name, false, Thread.NORM_PRIORITY);
    }

    public JNamedThreadFactory(String name, boolean daemon) {
        this(name, daemon, Thread.NORM_PRIORITY);
    }

    public JNamedThreadFactory(String name, int priority) {
        this(name, false, priority);
    }

    public JNamedThreadFactory(String name, boolean daemon, int priority) {
        this.name = DEFAULT_THREAD_NAME_PREFIX + name + "-thread-";
        this.daemon = daemon;
        this.priority = priority;
        this.group = Thread.currentThread().getThreadGroup(); // 直接获取当前线程组
    }

    @Override
    public Thread newThread(Runnable r) {
        Requires.requireNotNull(r, "runnable");

        String name2 = name + id.getAndIncrement();

        Runnable r2 = wrapRunnable(r);

        Thread t = wrapThread(group, r2, name2);

        try {
            if (t.isDaemon() != daemon) {
                t.setDaemon(daemon);
            }

            if (t.getPriority() != priority) {
                t.setPriority(priority);
            }
        } catch (Exception ignored) { /* doesn't matter even if failed to set. */ }

        return t;
    }

    public ThreadGroup getThreadGroup() {
        return group;
    }

    protected Runnable wrapRunnable(Runnable r) {
        return r; // InternalThreadLocalRunnable.wrap(r)
    }

    protected Thread wrapThread(ThreadGroup group, Runnable r, String name) {
        return new InternalThread(group, r, name);
    }
}
