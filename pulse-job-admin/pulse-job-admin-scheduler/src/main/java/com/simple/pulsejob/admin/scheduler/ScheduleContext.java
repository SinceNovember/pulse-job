package com.simple.pulsejob.admin.scheduler;

import com.simple.plusejob.serialization.SerializerType;
import com.simple.pulsejob.admin.scheduler.cluster.ClusterInvoker;
import com.simple.pulsejob.admin.scheduler.dispatch.Dispatcher;
import com.simple.pulsejob.admin.scheduler.load.balance.LoadBalancer;
import com.simple.pulsejob.transport.metadata.ExecutorKey;
import lombok.Data;

@Data
public class ScheduleContext {

    private ExecutorKey executorKey;

    private ClusterInvoker invoker;

    private boolean sync;

    private Dispatcher.Type dispatchType;

    private LoadBalancer.Type loadBalanceType;

    private SerializerType serializerType;

    private int retries;

    private Object result;


    public ScheduleContext(ExecutorKey executorKey, ClusterInvoker invoker, boolean sync) {
        this.executorKey = executorKey;
        this.invoker = invoker;
        this.sync = sync;
        this.dispatchType = Dispatcher.Type.BROADCAST;
        this.loadBalanceType = LoadBalancer.Type.RANDOM;
        this.serializerType = SerializerType.JAVA;
        this.retries = 1;
    }
}
