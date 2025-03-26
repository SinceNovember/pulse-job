package com.simple.pulsejob.client.registry;

import java.lang.reflect.Method;
import com.simple.pulsejob.client.annonation.JobRegister;


public class JobBeanDefinition {

    public JobBeanDefinition(Object targetBean, Method targetMethod, JobRegister jobRegister) {
        this.targetBean = targetBean;
        this.targetMethod = targetMethod;
        this.jobRegister = jobRegister;
    }
    private Object targetBean;

    private Method targetMethod;

    private JobRegister jobRegister;

    private String initMethodName;

    private String destroyMethodName;

    public Object getTargetBean() {
        return targetBean;
    }

    public void setTargetBean(Object targetBean) {
        this.targetBean = targetBean;
    }

    public Method getTargetMethod() {
        return targetMethod;
    }

    public void setTargetMethod(Method targetMethod) {
        this.targetMethod = targetMethod;
    }

    public JobRegister getJobRegister() {
        return jobRegister;
    }

    public void setJobRegister(JobRegister jobRegister) {
        this.jobRegister = jobRegister;
    }

    public String getInitMethodName() {
        return initMethodName;
    }

    public void setInitMethodName(String initMethodName) {
        this.initMethodName = initMethodName;
    }

    public String getDestroyMethodName() {
        return destroyMethodName;
    }

    public void setDestroyMethodName(String destroyMethodName) {
        this.destroyMethodName = destroyMethodName;
    }
}
