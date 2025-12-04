package com.simple.pulsejob.admin.scheduler;

import com.simple.pulsejob.admin.scheduler.cluster.ClusterInvoker;
import com.simple.pulsejob.admin.scheduler.dispatch.DispatchType;
import lombok.Data;

@Data
public class ScheduleContext {

    private ClusterInvoker invoker;

    private boolean sync;

    private DispatchType dispatchType;

    private int retries;

    private Object result;


    public ScheduleContext(ClusterInvoker invoker, boolean sync) {
        this.invoker = invoker;
        this.sync = sync;
    }
}
