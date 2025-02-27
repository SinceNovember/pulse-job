package com.simple.pulsejob.common;

import com.simple.pulsejob.common.concurrent.PulseJobNamedThreadFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public interface Consts {

    int CPU_PROCESSORS = Runtime.getRuntime().availableProcessors();

    int DEFAULT_CORE_POOL_SIZE= CPU_PROCESSORS * 4;

    int DEFAULT_MAX_POOL_SIZE = CPU_PROCESSORS * 6;

    static ThreadPoolExecutor createDefaultExecutor() {
        return new ThreadPoolExecutor(
                DEFAULT_CORE_POOL_SIZE, DEFAULT_MAX_POOL_SIZE, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(), new PulseJobNamedThreadFactory());
    }

}
