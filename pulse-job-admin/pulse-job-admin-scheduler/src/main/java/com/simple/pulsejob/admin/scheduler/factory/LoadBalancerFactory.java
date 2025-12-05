package com.simple.pulsejob.admin.scheduler.factory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.simple.plusejob.serialization.Serializer;
import com.simple.pulsejob.admin.scheduler.dispatch.Dispatcher;
import com.simple.pulsejob.admin.scheduler.load.balance.LoadBalancer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
public class LoadBalancerFactory extends EnumBeanFactory<LoadBalancer.Type, LoadBalancer>{

    public LoadBalancerFactory(List<LoadBalancer> loadBalancers) {
        super(loadBalancers, LoadBalancer::type, "Duplicate LoadBalancer type detected: ");
    }
}
