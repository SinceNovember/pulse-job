package com.simple.pulsejob.admin.scheduler.cluster;

import com.simple.pulsejob.admin.common.model.enums.InvokeStrategyEnum;
import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import com.simple.pulsejob.admin.scheduler.future.InvokeFuture;

/**
 * 集群调用器接口.
 *
 * <p>不同的实现类对应不同的容错策略：</p>
 * <ul>
 *   <li>{@link FailfastClusterInvoker} - 快速失败</li>
 *   <li>{@link FailoverClusterInvoker} - 失败重试</li>
 *   <li>{@link FailsafeClusterInvoker} - 失败安全</li>
 * </ul>
 */
public interface ClusterInvoker {

    /**
     * 获取当前调用器对应的策略
     */
    InvokeStrategyEnum strategy();

    /**
     * 执行调用
     */
    InvokeFuture invoke(ScheduleContext context) throws Exception;

}
