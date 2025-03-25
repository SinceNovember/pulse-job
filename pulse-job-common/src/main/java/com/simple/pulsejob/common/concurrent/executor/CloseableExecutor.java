package com.simple.pulsejob.common.concurrent.executor;

public interface CloseableExecutor {

    void execute(Runnable task);

    void shutdown();

}
