package com.simple.pulsejob.client.registry;

import com.simple.pulsejob.client.annonation.JobRegister;
import com.simple.pulsejob.common.util.Reflects;
import com.simple.pulsejob.common.util.StringUtil;
import com.simple.pulsejob.common.util.Strings;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class JobRegistry implements SmartInitializingSingleton {

    private final Map<String, MethodHolder> registry = new ConcurrentHashMap<>();

    private final ApplicationContext applicationContext;

    public JobRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    static class MethodHolder {
        final Object bean;
        final Method method;

        MethodHolder(Object bean, Method method) {
            this.bean = bean;
            this.method = method;
        }
    }


    @Override
    public void afterSingletonsInstantiated() {
        scanAndRegister();
    }

    private void scanAndRegister() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Object bean;
            try {
                bean = applicationContext.getBean(beanName);
            } catch (Exception ex) {
                // 某些bean在此时可能无法获取（例如部分 lazy/条件bean），跳过它们
                continue;
            }
            Class<?> userClass = ClassUtils.getUserClass(bean.getClass()); // 支持代理
            // 查找带注解的方法
            Map<Method, JobRegister> methods = MethodIntrospector.selectMethods(
                    userClass,
                    (Method candidate) -> AnnotationUtils.findAnnotation(candidate, JobRegister.class));

            for (Map.Entry<Method, JobRegister> entry : methods.entrySet()) {
                Method method = BridgeMethodResolver.findBridgedMethod(entry.getKey());
                JobRegister ann = entry.getValue();
                String key = generateJobName(method, ann);
                if (StringUtil.isBlank(key)) {
                    throw new IllegalStateException("JobRegister value must not be empty, bean=" + beanName + ", method=" + method);
                }

                // 如果目标方法来自父类或接口，需要在调用时基于 bean 实例使用正确的 Method
                // 这里我们从 userClass 找到实际的 Method 对象（已是 candidate）
                registry.compute(key, (k, v) -> {
                    if (v != null) {
                        throw new IllegalStateException("Duplicate JobRegister value '" + key + "'");
                    }
                    return new MethodHolder(bean, method);
                });

            }
        }
    }

    private String generateJobName(Method method, JobRegister jobRegister) {
        String value = jobRegister.value();
        if (StringUtil.isNotBlank(value)) {
            return value;
        }

        return method.getDeclaringClass().getSimpleName()
            + Strings.HASH_SYMBOL + method.getName()
            + Arrays.stream(method.getParameterTypes())
            .map(Class::getSimpleName)
            .collect(Collectors.joining(Strings.COMMA, Strings.LEFT_PAREN, Strings.RIGHT_PAREN));
    }

    /**
     * 方便直接调用
     */
    public Object invoke(String jobName, Object... args) throws Exception {
        MethodHolder holder = registry.get(jobName);
        if (holder == null) {
            throw new IllegalArgumentException("No job registered for name: " + jobName);
        }

        Method method = holder.method;
        Object bean = holder.bean;

        Class<?>[] parameterTypes = method.getParameterTypes();

        return Reflects.fastInvoke(bean, method.getName(), parameterTypes, args);
    }

}
