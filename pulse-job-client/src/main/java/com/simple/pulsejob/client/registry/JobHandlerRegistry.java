package com.simple.pulsejob.client.registry;

import com.simple.pulsejob.client.annonation.JobRegister;
import com.simple.pulsejob.common.util.JobNameResolver;
import com.simple.pulsejob.common.util.StringUtil;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

                // 使用框架级命名解析器
                String jobHandlerName = JobNameResolver.resolve(targetMethod, jobRegister.value());
                if (StringUtil.isBlank(jobHandlerName)) {
                    throw new IllegalStateException(
                        "JobRegister value must not be empty, bean=" + beanName + ", method=" + targetMethod);
                }

                handlerMap.compute(jobHandlerName, (name, existing) -> {
                    if (existing != null) {
                        throw new IllegalStateException(
                            "Duplicate JobRegister value '" + name + "'");
                    }
                    return new JobHandlerHolder(targetBean, targetMethod);
                });
            }
        }
    }

    public JobHandlerHolder getJobHandlerHolder(String jobHandlerName) {
        return handlerMap.get(jobHandlerName);
    }

}
