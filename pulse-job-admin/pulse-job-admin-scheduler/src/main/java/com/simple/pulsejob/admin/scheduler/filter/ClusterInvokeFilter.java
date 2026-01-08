package com.simple.pulsejob.admin.scheduler.filter;

import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import com.simple.pulsejob.admin.scheduler.cluster.ClusterInvoker;
import com.simple.pulsejob.admin.scheduler.future.InvokeFuture;
import com.simple.pulsejob.transport.JRequest;
import org.springframework.stereotype.Component;

@Component
public class ClusterInvokeFilter implements JobFilter {
    @Override
    public void doFilter(JRequest request, ScheduleContext context, JobFilterChain next) throws Throwable {
//
//        ClusterInvoker invoker = context.getInvoker();
//        InvokeFuture invoke = invoker.invoke(context);
//        if (context.isSync()) {
//            context.setResult(invoke.getResult());
//        }
//        else  {
//            context.setResult(invoke);
//        }
//        if (next != null) {
//            next.doFilter(request, context);
//        }
    }
}
