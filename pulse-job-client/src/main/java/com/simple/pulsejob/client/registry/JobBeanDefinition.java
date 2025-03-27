package com.simple.pulsejob.client.registry;

import java.lang.reflect.Method;
import com.simple.pulsejob.client.annonation.JobRegister;
import lombok.Getter;


@Getter
public class JobBeanDefinition {

    private final Object targetBean;

    private final Method targetMethod;

    private final JobRegister jobRegister;

    public JobBeanDefinition(Object targetBean, Method targetMethod, JobRegister jobRegister) {
        this.targetBean = targetBean;
        this.targetMethod = targetMethod;
        this.jobRegister = jobRegister;
    }

    public String getTargetInitMethodName() {
        return jobRegister.init();
    }

    public String getTargetDestroyMethodName() {
        return jobRegister.destroy();
    }
}
