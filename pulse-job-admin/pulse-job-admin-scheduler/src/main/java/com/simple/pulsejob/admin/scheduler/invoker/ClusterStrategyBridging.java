package com.simple.pulsejob.admin.scheduler.invoker;

import com.simple.pulsejob.admin.scheduler.cluster.ClusterInvoker;
import com.simple.pulsejob.admin.scheduler.dispatch.DispatchType;
import com.simple.pulsejob.admin.scheduler.dispatch.Dispatcher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ClusterStrategyBridging {
    private final Map<DispatchType, Map<ClusterInvoker.Strategy, ObjectProvider<? extends ClusterInvoker>>> providerMap = new ConcurrentHashMap<>();

    public ClusterStrategyBridging(ApplicationContext context,
                                 List<ClusterInvoker> invokers,
                                 List<Dispatcher> dispatchers) {

        // 初始化 DispatchType Map
        for (Dispatcher dispatcher : dispatchers) {
            providerMap.put(dispatcher.type(), new EnumMap<>(ClusterInvoker.Strategy.class));
        }

        // 初始化 ClusterInvoker 映射
        for (ClusterInvoker invoker : invokers) {
            ClusterInvoker.Strategy strategy = invoker.strategy();

            for (DispatchType dispatchType : providerMap.keySet()) {
                ObjectProvider<? extends ClusterInvoker> provider =
                        context.getBeanProvider(invoker.getClass());
                providerMap.get(dispatchType).put(strategy, provider);
            }
        }
    }

    public ClusterInvoker getInvoker(DispatchType dispatchType, ClusterInvoker.Strategy strategy) {
        Map<ClusterInvoker.Strategy, ObjectProvider<? extends ClusterInvoker>> strategyMap = providerMap.get(dispatchType);
        if (strategyMap == null) {
            throw new IllegalArgumentException("Unsupported dispatcher type: " + dispatchType);
        }

        ObjectProvider<? extends ClusterInvoker> provider = strategyMap.get(strategy);
        if (provider == null) {
            throw new IllegalArgumentException("Unsupported cluster strategy: " + strategy);
        }

        // 每次返回新的实例（如果是 prototype），或者同一实例（如果是 singleton）
        return provider.getObject();
    }
}



