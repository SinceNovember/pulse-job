package com.simple.pulsejob.client.autoconfigure;

import java.util.HashMap;
import java.util.Map;
import com.simple.plusejob.serialization.Serializer;
import com.simple.plusejob.serialization.SerializerType;
import com.simple.pulsejob.client.DefaultClient;
import com.simple.pulsejob.client.invoker.DefaultInvoker;
import com.simple.pulsejob.client.invoker.Invoker;
import com.simple.pulsejob.client.log.CustomLogAppenderInitializer;
import com.simple.pulsejob.client.processor.DefaultClientProcessor;
import com.simple.pulsejob.client.registry.JobAutoRegister;
import com.simple.pulsejob.client.registry.JobBeanDefinitionRegistry;
import com.simple.pulsejob.common.concurrent.executor.CloseableExecutor;
import com.simple.pulsejob.common.concurrent.executor.ExecutorFactory;
import com.simple.pulsejob.serialization.hessian.HessianSerializer;
import com.simple.pulsejob.serialization.java.JavaSerializer;
import com.simple.pulsejob.transport.processor.ConnectorProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@RequiredArgsConstructor
@Import({JobAutoRegister.class, CustomLogAppenderInitializer.class})
@EnableConfigurationProperties(PulseJobClientProperties.class)
public class PulseJobClientConfiguration {

    private final PulseJobClientProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public CloseableExecutor closeableExecutor() {
        return ExecutorFactory.getExecutorFactory(properties.getExecutor())
            .newExecutor(properties.getCorePoolSize(), properties.getMaxPoolSize(), properties.getWorkQueue());
    }

    @Bean
    @ConditionalOnMissingBean
    public JobBeanDefinitionRegistry jobBeanDefinitionRegistry() {
        return new JobBeanDefinitionRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public Invoker invoker() {
        return new DefaultInvoker();
    }

    /**
     * 使用 Map<Byte, Serializer> 直接存储
     */
    @Bean
    public Map<Byte, Serializer> serializerMap() {
        Map<Byte, Serializer> serializerMap = new HashMap<>();
        serializerMap.put(SerializerType.JAVA.value(), new JavaSerializer());
        serializerMap.put(SerializerType.HESSIAN.value(), new HessianSerializer());
        return serializerMap;
    }

    @Bean
    public ConnectorProcessor connectorProcessor(CloseableExecutor closeableExecutor,
                                                 Map<Byte, Serializer> serializerMap,
                                                 JobBeanDefinitionRegistry jobBeanDefinitionRegistry,
                                                 Invoker invoker) {
        return new DefaultClientProcessor(closeableExecutor, serializerMap, jobBeanDefinitionRegistry, invoker, properties);
    }

    @Bean
    public DefaultClient defaultClient(PulseJobClientProperties properties,
                                       ConnectorProcessor connectorProcessor,
                                       Map<Byte, Serializer> serializerMap) {

        return new DefaultClient(properties, connectorProcessor, serializerMap);
    }

}

