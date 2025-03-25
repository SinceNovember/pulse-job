package com.simple.pulsejob.common.concurrent.executor.disruptor;

import com.simple.pulsejob.common.JConstants;

public interface Dispatcher<T> {
    int BUFFER_SIZE = 32768;
    int MAX_NUM_WORKERS = JConstants.AVAILABLE_PROCESSORS << 3;

    boolean dispatch(T message);

    void shutdown();

}
