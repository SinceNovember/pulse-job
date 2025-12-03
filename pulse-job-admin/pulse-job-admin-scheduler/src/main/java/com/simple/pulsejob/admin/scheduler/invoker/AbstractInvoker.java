package com.simple.pulsejob.admin.scheduler.invoker;

import com.simple.pulsejob.admin.scheduler.cluster.ClusterInvoker;
import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.metadata.MessageWrapper;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public abstract class AbstractInvoker {

    private final ClusterInvoker clusterInvoker;

    protected Object doInvoke(String handlerName, String args, boolean sync) {
        JRequest request = createRequest(handlerName, args);
        return null;
    }

    private JRequest createRequest(String handlerName, String args) {
        MessageWrapper message = new MessageWrapper(handlerName, args);

        JRequest request = new JRequest();
        request.setMessage(message);
        return request;
    }
}
