package com.simple.pulsejob.admin.scheduler.dispatch;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DispatcherFactory {

    private final Map<DispatchType, ObjectProvider<Dispatcher>> dispatcherProviderMap = new ConcurrentHashMap<>();

    public DispatcherFactory(List<ObjectProvider<Dispatcher>> providers) {
        for (ObjectProvider<Dispatcher> provider : providers) {
            Dispatcher dispatcher = provider.getIfAvailable();
            if (dispatcher != null) {
                dispatcherProviderMap.put(dispatcher.type(), provider);
            }
        }
    }

    public Dispatcher get(DispatchType type) {
        ObjectProvider<Dispatcher> provider = dispatcherProviderMap.get(type);
        if (provider == null) {
            throw new IllegalArgumentException("No Dispatcher for type: " + type);
        }
        return provider.getObject();
    }
}