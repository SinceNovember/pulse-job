package com.simple.pulsejob.client.exception;

public class ConflictingJobBeanDefinitionException extends IllegalStateException {
    public ConflictingJobBeanDefinitionException(String message) {
        super(message);
    }
}
