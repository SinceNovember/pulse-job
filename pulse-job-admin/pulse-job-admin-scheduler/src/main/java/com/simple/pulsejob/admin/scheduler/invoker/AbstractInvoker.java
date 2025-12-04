package com.simple.pulsejob.admin.scheduler.invoker;

import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import com.simple.pulsejob.admin.scheduler.cluster.ClusterInvoker;
import com.simple.pulsejob.admin.scheduler.filter.JobFilterChains;
import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.metadata.MessageWrapper;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public abstract class AbstractInvoker {

    private final ClusterInvoker clusterInvoker;

    private final JobFilterChains chains;

    protected Object doInvoke(String handlerName, String args, boolean sync) throws Throwable {
        JRequest request = createRequest(handlerName, args);
        ScheduleContext context = new ScheduleContext(clusterInvoker, sync);
        chains.doFilter(request, context);
        return context.getResult();
    }

    private JRequest createRequest(String handlerName, String args) {
        MessageWrapper message = new MessageWrapper(handlerName, args);

        JRequest request = new JRequest();
        request.setMessage(message);
        return request;
    }
}
