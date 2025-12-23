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
public class JobHandlerRegistry implements SmartInitializingSingleton {

    private final Map<String, JobHandlerHolder> handlerMap = new ConcurrentHashMap<>();

    private final ApplicationContext applicationContext;

    public JobHandlerRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterSingletonsInstantiated() {
        scanAndRegister();
    }

    private void scanAndRegister() {
        String[] candidateBeanNames = applicationContext.getBeanDefinitionNames();

        for (String beanName : candidateBeanNames) {
            Object targetBean;
            try {
                targetBean = applicationContext.getBean(beanName);
            } catch (Exception ex) {
                // 某些 bean 在此阶段可能无法实例化（lazy / conditional）
                continue;
            }

            Class<?> targetClass = ClassUtils.getUserClass(targetBean.getClass());

            Map<Method, JobRegister> annotatedMethods =
                MethodIntrospector.selectMethods(
                    targetClass,
                    (Method method) -> AnnotationUtils.findAnnotation(method, JobRegister.class)
                );

            for (Map.Entry<Method, JobRegister> methodEntry : annotatedMethods.entrySet()) {
                Method targetMethod =
                    BridgeMethodResolver.findBridgedMethod(methodEntry.getKey());

                JobRegister jobRegister = methodEntry.getValue();

                String jobHandlerName = resolveJobName(targetMethod, jobRegister);
                if (StringUtil.isBlank(jobHandlerName)) {
                    throw new IllegalStateException(
                        "JobRegister value must not be empty, bean=" + beanName + ", method=" + targetMethod
                    );
                }

                handlerMap.compute(jobHandlerName, (name, existing) -> {
                    if (existing != null) {
                        throw new IllegalStateException(
                            "Duplicate JobRegister value '" + name + "'"
                        );
                    }
                    return new JobHandlerHolder(targetBean, targetMethod);
                });
            }
        }
    }

    private String resolveJobName(Method method, JobRegister jobRegister) {
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


    public JobHandlerHolder getJobHandlerHolder(String jobHandlerName) {
        return handlerMap.get(jobHandlerName);
    }

}
