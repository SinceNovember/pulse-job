package com.simple.pulsejob.admin.scheduler.invoker;

import com.simple.pulsejob.admin.scheduler.cluster.ClusterInvoker;
import com.simple.pulsejob.admin.scheduler.filter.JobFilterChains;
import org.springframework.stereotype.Component;

@Component
public class AutoInvoker extends AbstractInvoker {

    public AutoInvoker(ClusterInvoker clusterInvoker, JobFilterChains chains) {
        super(clusterInvoker, chains);
    }

    @Override
    public Object invoke(String executorName, String handlerName, String args) throws Throwable {
        return doInvoke(executorName, handlerName, args, false);
    }
}
