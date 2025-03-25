package com.simple.pulsejob.client.registry;

import java.lang.reflect.Method;
import com.simple.pulsejob.client.annonation.JobRegister;

public class JobBeanDefinition {

    private long jobId;

    private Object targetBean;

    private Method targetMethod;

    private JobRegister jobRegister;

    public long getJobId() {
        return jobId;
    }

    public void setJobId(long jobId) {
        this.jobId = jobId;
    }

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
}
