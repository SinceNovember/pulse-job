package com.simple.pulsejob.client;

import com.simple.pulsejob.transport.metadata.MessageWrapper;
import com.simple.pulsejob.client.registry.JobBeanDefinition;
import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.channel.JChannel;

public class JobContext {

    private final JChannel channel;
    private final JRequest request;

    private String handlerName;

    private final String args;                  // 目标方法参数

    private Object result;

    private Throwable cause;

    public JobContext(JChannel channel, JRequest request,
                      MessageWrapper messageWrapper) {
        this.channel = channel;
        this.request = request;
        this.handlerName = messageWrapper.getHandlerName();
        this.args = messageWrapper.getArgs();
    }

    public String getHandlerName() {
        return handlerName;
    }

    public void setHandlerName(String handlerName) {
        this.handlerName = handlerName;
    }

    public String getArgs() {
        return args;
    }

    public String args() {
        return args;
    }

    public long invokeId() {
        return request.invokeId();
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
