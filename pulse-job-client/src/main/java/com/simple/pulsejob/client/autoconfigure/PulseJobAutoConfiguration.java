package com.simple.pulsejob.client.autoconfigure;

import com.simple.plusejob.serialization.Serializer;
import com.simple.pulsejob.common.concurrent.executor.CloseableExecutor;
import com.simple.pulsejob.common.concurrent.executor.DisruptorExecutorFactory;
import com.simple.pulsejob.common.concurrent.executor.ExecutorFactory;
import com.simple.pulsejob.common.concurrent.executor.ThreadPoolExecutorFactory;
import com.simple.pulsejob.common.util.StringUtil;
import com.simple.pulsejob.serialization.hessian.HessianSerializer;
import com.simple.pulsejob.serialization.java.JavaSerializer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(PulseJobProperties.class)
public class PulseJobAutoConfiguration {

    private final PulseJobProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public CloseableExecutor closeableExecutor() {
        return ExecutorFactory.getExecutorFactory(properties.getExecutor())
            .newExecutor(properties.getCorePoolSize(), properties.getMaxPoolSize(), properties.getWorkQueue());
    }

    @Bean
    public Serializer javaSerializer() {
        return new JavaSerializer();
    }

    @Bean
    public Serializer hessianSerializer() {
        return new HessianSerializer();
    }


}

