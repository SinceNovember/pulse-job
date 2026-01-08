package com.simple.pulsejob.admin.scheduler.factory;

import com.simple.pulsejob.admin.common.model.enums.DispatchTypeEnum;
import com.simple.pulsejob.admin.scheduler.dispatch.Dispatcher;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DispatcherFactory extends EnumBeanFactory<DispatchTypeEnum, Dispatcher> {

    public DispatcherFactory(List<Dispatcher> dispatchers) {
        super(dispatchers, Dispatcher::type, "Duplicate Dispatcher type detected: ");
    }
}
