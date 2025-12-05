package com.simple.pulsejob.admin.scheduler.invoker;

public interface Invoker {

    Object invoke(String executorName, String handlerName, String args) throws Throwable;
}
