package com.simple.pulsejob.common.concurrent.executor;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import com.simple.pulsejob.common.concurrent.JNamedThreadFactory;

public abstract class AbstractExecutorFactory implements ExecutorFactory {

    protected ThreadFactory threadFactory() {
        return new JNamedThreadFactory();
    }

    protected BlockingQueue<Runnable>  workQueue(int queueCapacity) {
        return new ArrayBlockingQueue<>(queueCapacity);
    }

}
