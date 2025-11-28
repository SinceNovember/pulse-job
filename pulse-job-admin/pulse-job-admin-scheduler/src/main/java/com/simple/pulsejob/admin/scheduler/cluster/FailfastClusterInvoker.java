package com.simple.pulsejob.admin.scheduler.cluster;

import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import com.simple.pulsejob.admin.scheduler.dispatch.Dispatcher;
import com.simple.pulsejob.admin.scheduler.future.InvokeFuture;
import com.simple.pulsejob.transport.JRequest;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class FailfastClusterInvoker implements ClusterInvoker {

    private final Dispatcher dispatcher;

    @Override
    public Strategy strategy() {
        return Strategy.FAIL_FAST;
    }

    @Override
    public InvokeFuture invoke(JRequest request, ScheduleContext scheduleContext) throws Exception {
        return dispatcher.dispatch(request);
    }

}
