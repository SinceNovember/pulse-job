package com.simple.pulsejob.common;

import com.simple.pulsejob.common.concurrent.JNamedThreadFactory;
import com.simple.pulsejob.common.util.SystemPropertyUtil;

import java.util.Formatter;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public interface JConstants {

   String DEFAULT_THREAD_NAME_PREFIX = "pulse-job-";

    int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();

    int DEFAULT_CORE_POOL_SIZE= AVAILABLE_PROCESSORS * 4;

    int DEFAULT_MAX_POOL_SIZE = AVAILABLE_PROCESSORS * 6;

    boolean CODEC_LOW_COPY =
        SystemPropertyUtil.getBoolean("jupiter.io.codec.low_copy", true);

    /**
     * 可配置的 available processors. 默认值是 {@link Runtime#availableProcessors()}.
     * 可以通过设置 system property "jupiter.available_processors" 来覆盖默认值.
     */
//    int AVAILABLE_PROCESSORS =
//        SystemPropertyUtil.getInt("jupiter.available_processors", Runtime.getRuntime().availableProcessors());

    int EXPLICIT_FLUSH_AFTER_FLUSHES =
        SystemPropertyUtil.getInt("jupiter.io.explicit.flush.after.flushes", 1024);

    int READER_IDLE_TIME_SECONDS =
        SystemPropertyUtil.getInt("jupiter.io.reader.idle.time.seconds", 60);

    int WRITER_IDLE_TIME_SECONDS =
        SystemPropertyUtil.getInt("jupiter.io.writer.idle.time.seconds", 30);

    static ThreadPoolExecutor createDefaultExecutor() {
        return new ThreadPoolExecutor(
                DEFAULT_CORE_POOL_SIZE, DEFAULT_MAX_POOL_SIZE, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(), new JNamedThreadFactory());
    }



}
