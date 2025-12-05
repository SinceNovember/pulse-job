package com.simple.pulsejob.client;

import com.simple.pulsejob.transport.metadata.MessageWrapper;
import com.simple.pulsejob.client.registry.JobBeanDefinition;
import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.channel.JChannel;

public class JobContext {

    private final JChannel channel;
    private final JRequest request;

    private final JobBeanDefinition jobBeanDefinition;

    private final String args;                  // 目标方法参数

    private Object result;

    private Throwable cause;

    public JobContext(JChannel channel, JRequest request,
                      MessageWrapper messageWrapper, JobBeanDefinition jobBeanDefinition) {
        this.channel = channel;
        this.request = request;
        this.args = messageWrapper.getArgs();
        this.jobBeanDefinition = jobBeanDefinition;
    }

    public Object targetBean() {
        return jobBeanDefinition.getTargetBean();
    }

    public String targetMethodName() {
        return jobBeanDefinition.getTargetMethod().getName();
    }

    public Class<?>[] parameterTypes() {
        return jobBeanDefinition.getTargetMethod().getParameterTypes();
    }

    public String args() {
        return args;
    }

    public long invokeId() {
        return request.invokeId();
    }

    public String targetInitMethodName() {
        return jobBeanDefinition.getTargetInitMethodName();
    }

    public String targetDestroyMethodName() {
        return jobBeanDefinition.getTargetDestroyMethodName();
    }

    public JChannel channel() {
        return channel;
    }

    public JRequest request() {
        return request;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public void setCause(Throwable cause) {
        this.cause = cause;
    }
}
