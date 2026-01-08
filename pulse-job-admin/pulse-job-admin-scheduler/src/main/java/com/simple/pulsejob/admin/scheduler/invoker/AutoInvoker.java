package com.simple.pulsejob.admin.scheduler.invoker;

import com.simple.pulsejob.admin.scheduler.ScheduleConfig;
import com.simple.pulsejob.admin.scheduler.cluster.ClusterInvoker;
import com.simple.pulsejob.admin.scheduler.interceptor.SchedulerInterceptorChain;
import org.springframework.stereotype.Component;

/**
 * 自动调用器.
 *
 * <p>每次调度都会创建 JobInstance，使用 instanceId 作为请求标识</p>
 */
@Component
public class AutoInvoker extends AbstractInvoker {

    public AutoInvoker(ClusterInvoker clusterInvoker,
                       SchedulerInterceptorChain schedulerInterceptorChain) {
        super(clusterInvoker, schedulerInterceptorChain);
    }

    @Override
    public Object invoke(ScheduleConfig config) throws Throwable {
        return doInvoke(config);
    }
}
