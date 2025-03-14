package com.simple.pulsejob.common.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class JNamedThreadFactory implements ThreadFactory {

    private static final String DEFAULT_THREAD_NAME_PREFIX = "pulse-job-";
    private static final String DEFAULT_THREAD_POOL_NAME = "default";
    private final String namePrefix;
    private final AtomicInteger id = new AtomicInteger();

    public JNamedThreadFactory() {
        this(DEFAULT_THREAD_POOL_NAME);
    }

    public JNamedThreadFactory(String poolName) {
        namePrefix = DEFAULT_THREAD_NAME_PREFIX + poolName + "-thread-";
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, namePrefix + id.getAndIncrement());
        t.setDaemon(true);
        return t;
    }
}
