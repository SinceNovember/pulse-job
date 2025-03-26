package com.simple.pulsejob.client.registry;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.simple.pulsejob.client.annonation.JobRegister;
import com.simple.pulsejob.client.exception.ConflictingJobBeanDefinitionException;
import com.simple.pulsejob.common.util.StringUtil;
import com.simple.pulsejob.common.util.Strings;
import com.simple.pulsejob.common.util.internal.logging.InternalLogger;
import com.simple.pulsejob.common.util.internal.logging.InternalLoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

public class JobBeanDefinitionRegistry implements BeanPostProcessor, ApplicationContextAware {

    private InternalLogger logger = InternalLoggerFactory.getInstance(JobBeanDefinitionRegistry.class);

    private final Map<String, JobBeanDefinition> jobBeanDefinitionMap = new ConcurrentHashMap<>(256);

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        ReflectionUtils.doWithMethods(bean.getClass(), method -> {
            JobRegister jobRegister = AnnotationUtils.findAnnotation(method, JobRegister.class);
            registerJobMethod(bean, method, jobRegister);
        });
        return bean;
    }

    private void registerJobMethod(Object bean, Method method, JobRegister jobRegister) {
        validateMethodSignature(method);
        String jobBeanDefinitionName = generateJobBeanDefinitionName(bean.getClass(), method, jobRegister);
        if (jobBeanDefinitionMap.containsKey(jobBeanDefinitionName)) {
            throw new ConflictingJobBeanDefinitionException("job名称" + jobBeanDefinitionName + "存在冲突！");
        }
        JobBeanDefinition jobBeanDefinition = new JobBeanDefinition(bean, method, jobRegister);
        jobBeanDefinitionMap.put(jobBeanDefinitionName, jobBeanDefinition);

        if (logger.isDebugEnabled()) {
            logger.debug("Registered scheduled task: ClassName={}, Method={}", bean.getClass(), method);
        }
    }

    private String generateJobBeanDefinitionName(Class<?> clazz, Method method, JobRegister jobRegister) {
        String value = jobRegister.value();
        //在注解中定义了名称的话直接使用，否则用类名#方法名来生成名称
        if (StringUtil.isNotBlank(value)) {
            return value;
        }
        return clazz.getSimpleName() + Strings.HASH_SYMBOL + method.getName();
    }

    public JobBeanDefinition getJobBeanDefinition(String jobBeanDefinitionName) {
        return jobBeanDefinitionMap.get(jobBeanDefinitionName);
    }

    /**
     * 验证方法签名
     */
    private void validateMethodSignature(Method method) {
        // 检查方法权限
        Assert.isTrue(Modifier.isPublic(method.getModifiers()), "Scheduled task method must be public");
    }
}
