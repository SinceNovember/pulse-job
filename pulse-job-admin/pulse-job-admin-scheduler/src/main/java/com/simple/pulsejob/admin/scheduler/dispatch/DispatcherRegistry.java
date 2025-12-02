package com.simple.pulsejob.admin.scheduler.dispatch;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DispatcherRegistry {

    private final Map<DispatchType, ObjectProvider<Dispatcher>> registry = new ConcurrentHashMap<>();

    public DispatcherRegistry(List<ObjectProvider<Dispatcher>> providers) {
        for (ObjectProvider<Dispatcher> provider : providers) {
            Dispatcher dispatcher = provider.getIfAvailable();
            registry.put(dispatcher.type(), provider);
        }
    }

    public Dispatcher get(DispatchType type) {
        ObjectProvider<Dispatcher> provider = registry.get(type);
        if (provider == null) {
            throw new IllegalArgumentException("No Dispatcher for type: " + type);
        }
        return provider.getObject();
    }
}