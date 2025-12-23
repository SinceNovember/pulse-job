package com.simple.pulsejob.client.registry;

import java.lang.reflect.Method;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JobHandlerHolder {

    private Object bean;

    private Method method;
}
