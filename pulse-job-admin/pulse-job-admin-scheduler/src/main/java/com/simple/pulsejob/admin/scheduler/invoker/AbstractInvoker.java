package com.simple.pulsejob.admin.scheduler.invoker;

import com.simple.plusejob.serialization.SerializerType;
import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import com.simple.pulsejob.admin.scheduler.cluster.ClusterInvoker;
import com.simple.pulsejob.admin.scheduler.filter.JobFilterChains;
import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.metadata.ExecutorKey;
import com.simple.pulsejob.transport.metadata.MessageWrapper;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;

@RequiredArgsConstructor
public abstract class AbstractInvoker implements Invoker {

    private final ClusterInvoker clusterInvoker;

    private final JobFilterChains chains;

    protected Object doInvoke(String executorName, String handlerName, String args, boolean sync) throws Throwable {
        JRequest request = createRequest(handlerName, args);
        ScheduleContext context = new ScheduleContext(ExecutorKey.of(executorName), clusterInvoker, sync);
        chains.doFilter(request, context);
        return context.getResult();
    }

    private JRequest createRequest(String handlerName, String args) {
        MessageWrapper message = new MessageWrapper(handlerName, args);

        JRequest request = new JRequest();
        request.setMessage(message);
        request.getPayload().setSerializerCode(SerializerType.JAVA.value());
        return request;
    }
}
