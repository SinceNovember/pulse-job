package com.simple.pulsejob.client.log;

import com.simple.pulsejob.client.JobContext;
import org.slf4j.MDC;

public final class LogMDCScope implements AutoCloseable {

    public static final String INSTANCE_ID = "instanceId";

    public static final String TASK_ID = "taskId";

    public static LogMDCScope open(JobContext ctx) {
        MDC.put(INSTANCE_ID, String.valueOf(ctx.instanceId()));
        MDC.put(TASK_ID, String.valueOf(ctx.instanceId()));
        return new LogMDCScope();
    }

    @Override
    public void close() {
        MDC.remove(INSTANCE_ID);
        MDC.remove(TASK_ID);
    }
}