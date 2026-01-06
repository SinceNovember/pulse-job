package com.simple.pulsejob.admin.scheduler.invoker;

import com.simple.pulsejob.admin.scheduler.cluster.ClusterInvoker;
import com.simple.pulsejob.admin.scheduler.filter.JobFilterChains;
import com.simple.pulsejob.admin.scheduler.instance.JobInstanceManager;
import org.springframework.stereotype.Component;

/**
 * 自动调用器.
 *
 * <p>每次调度都会创建 JobInstance，使用 instanceId 作为请求标识</p>
 */
@Component
public class AutoInvoker extends AbstractInvoker {

    public AutoInvoker(ClusterInvoker clusterInvoker,
                       JobFilterChains chains,
                       JobInstanceManager jobInstanceManager) {
        super(clusterInvoker, chains, jobInstanceManager);
    }

    @Override
    public Object invoke(String executorName, Long jobId, Long executorId,
                         String handlerName, String args) throws Throwable {
        return doInvoke(executorName, jobId, executorId, handlerName, args, false);
    }
}
