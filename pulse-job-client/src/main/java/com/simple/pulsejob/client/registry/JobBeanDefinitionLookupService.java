package com.simple.pulsejob.client.registry;

public interface JobBeanDefinitionLookupService {

    JobBeanDefinition getJobBeanDefinition(String jobBeanDefinitionName);
}
