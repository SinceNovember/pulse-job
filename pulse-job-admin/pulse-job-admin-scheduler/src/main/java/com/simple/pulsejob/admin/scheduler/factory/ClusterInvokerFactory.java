package com.simple.pulsejob.admin.scheduler.factory;

import com.simple.pulsejob.admin.common.model.enums.InvokeStrategyEnum;
import com.simple.pulsejob.admin.scheduler.cluster.ClusterInvoker;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 集群调用器工厂.
 *
 * <p>根据 {@link InvokeStrategyEnum} 获取对应的 {@link ClusterInvoker} 实现.</p>
 */
@Component
public class ClusterInvokerFactory extends EnumBeanFactory<InvokeStrategyEnum, ClusterInvoker> {

    public ClusterInvokerFactory(List<ClusterInvoker> invokers) {
        super(invokers, ClusterInvoker::strategy, "Duplicate ClusterInvoker strategy detected: ");
    }
}
