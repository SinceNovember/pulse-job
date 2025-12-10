package com.simple.pulsejob.client.registry;

import com.simple.pulsejob.client.annonation.JobRegister;
import com.simple.pulsejob.common.util.Reflects;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JobRegistry implements SmartInitializingSingleton {


    private final Map<String, Method> registry = new ConcurrentHashMap<>();

    private final ApplicationContext applicationContext;

    public JobRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
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
                Method method = entry.getKey();
                JobRegister ann = entry.getValue();
                String key = ann.value();
                if (key == null || key.trim().isEmpty()) {
                    throw new IllegalStateException("JobRegister value must not be empty, bean=" + beanName + ", method=" + method);
                }

                // 如果目标方法来自父类或接口，需要在调用时基于 bean 实例使用正确的 Method
                // 这里我们从 userClass 找到实际的 Method 对象（已是 candidate）

                Method existing = registry.putIfAbsent(key, method);
                if (existing != null) {
                    throw new IllegalStateException("Duplicate JobRegister value '" + key + "' found. " +
                            "Existing: " + existing + ", New: " + method);
                }
            }
        }
    }



    /**
     * 方便直接调用
     */
    public Object invoke(String key, Object... args) throws Exception {

    }

}
