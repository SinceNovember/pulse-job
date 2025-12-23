package com.simple.pulsejob.client;

import com.simple.pulsejob.transport.metadata.MessageWrapper;
import com.simple.pulsejob.client.registry.JobBeanDefinition;
import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.channel.JChannel;
import lombok.Data;

@Data
public class JobContext {

    private final JChannel channel;
    private final JRequest request;

    private String handlerName;

    private Object[] args;

    private Object result;

    private Throwable cause;

    public JobContext(JChannel channel, JRequest request,
                      MessageWrapper messageWrapper) {
        this.channel = channel;
        this.request = request;
        this.handlerName = messageWrapper.getHandlerName();
    }

    public Long invokeId() {
        return request.invokeId();
    }

}
