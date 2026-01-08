package com.simple.pulsejob.admin.scheduler.factory;

import com.simple.pulsejob.admin.common.model.enums.LoadBalanceTypeEnum;
import com.simple.pulsejob.admin.scheduler.load.balance.LoadBalancer;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LoadBalancerFactory extends EnumBeanFactory<LoadBalanceTypeEnum, LoadBalancer> {

    public LoadBalancerFactory(List<LoadBalancer> loadBalancers) {
        super(loadBalancers, LoadBalancer::type, "Duplicate LoadBalancer type detected: ");
    }
}
