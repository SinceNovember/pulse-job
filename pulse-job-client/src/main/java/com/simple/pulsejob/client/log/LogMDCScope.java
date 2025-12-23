package com.simple.pulsejob.client.log;

import com.simple.pulsejob.client.JobContext;
import org.slf4j.MDC;

public final class LogMDCScope implements AutoCloseable {

    public static final String INVOKE_ID = "invokeId";

    public static final String TASK_ID = "taskId";

    public static LogMDCScope open(JobContext ctx) {
        MDC.put(INVOKE_ID, String.valueOf(ctx.invokeId()));
        MDC.put(TASK_ID, String.valueOf(ctx.invokeId()));
        return new LogMDCScope();
    }

    @Override
    public void close() {
        MDC.remove(INVOKE_ID);
        MDC.remove(TASK_ID);
    }
}