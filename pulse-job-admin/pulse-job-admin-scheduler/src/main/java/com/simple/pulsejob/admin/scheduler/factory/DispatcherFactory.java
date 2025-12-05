package com.simple.pulsejob.admin.scheduler.factory;

import com.simple.pulsejob.admin.scheduler.dispatch.Dispatcher;
import com.simple.pulsejob.admin.scheduler.load.balance.LoadBalancer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DispatcherFactory extends EnumBeanFactory<Dispatcher.Type, Dispatcher> {

    public DispatcherFactory(List<Dispatcher> dispatchers) {
        super(dispatchers, Dispatcher::type, "Duplicate Dispatcher type detected: ");
    }

}