package com.simple.pulsejob.client.autoconfigure;

import com.simple.plusejob.serialization.Serializer;
import com.simple.pulsejob.client.DefaultClient;
import com.simple.pulsejob.client.interceptor.JobExecutionInterceptor;
import com.simple.pulsejob.client.interceptor.JobExecutionInterceptorChain;
import com.simple.pulsejob.client.interceptor.LoggingJobInterceptor;
import com.simple.pulsejob.client.invoker.DefaultInvoker;
import com.simple.pulsejob.client.invoker.Invoker;
import com.simple.pulsejob.client.log.CustomLogAppenderInitializer;
import com.simple.pulsejob.client.log.JobLogSender;
import com.simple.pulsejob.client.processor.DefaultClientProcessor;
import com.simple.pulsejob.client.registry.JobBeanDefinitionRegistry;
import com.simple.pulsejob.client.registry.JobHandlerRegistry;
import com.simple.pulsejob.client.serialization.SerializerHolder;
import com.simple.pulsejob.common.concurrent.executor.CloseableExecutor;
import com.simple.pulsejob.common.concurrent.executor.ExecutorFactory;
import com.simple.pulsejob.serialization.hessian.HessianSerializer;
import com.simple.pulsejob.serialization.java.JavaSerializer;
import com.simple.pulsejob.transport.processor.ConnectorProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.List;

/**
 * Pulse Job 客户端自动配置.
 *
 * <p>使用方式：引入 pulse-job-client 依赖，配置以下属性即可：</p>
 * <pre>{@code
 * pulse-job:
 *   enabled: true
 *   executor-name: my-executor
 *   admin:
 *     host: 127.0.0.1
 *     port: 9999
 * }</pre>
 *
 * <p>可通过 {@code pulse-job.enabled=false} 禁用自动配置</p>
 */
@AutoConfiguration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "pulse-job", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PulseJobClientProperties.class)
@Import({JobHandlerRegistry.class, CustomLogAppenderInitializer.class, LoggingJobInterceptor.class})
public class PulseJobClientConfiguration {

    private final PulseJobClientProperties properties;

    // ==================== 基础设施 ====================

    @Bean
    @ConditionalOnMissingBean
    public CloseableExecutor closeableExecutor() {
        return ExecutorFactory.getExecutorFactory(properties.getExecutor())
                .newExecutor(properties.getCorePoolSize(), properties.getMaxPoolSize(), properties.getWorkQueue());
    }

    // ==================== 序列化器 ====================

    @Bean
    @ConditionalOnMissingBean(JavaSerializer.class)
    public JavaSerializer javaSerializer() {
        return new JavaSerializer();
    }

    @Bean
    @ConditionalOnMissingBean(HessianSerializer.class)
    public HessianSerializer hessianSerializer() {
        return new HessianSerializer();
    }

    @Bean
    @ConditionalOnMissingBean
    public SerializerHolder serializerHolder(List<Serializer> serializers) {
        return new SerializerHolder(serializers);
    }

    // ==================== Job 注册相关 ====================

    @Bean
    @ConditionalOnMissingBean
    public JobBeanDefinitionRegistry jobBeanDefinitionRegistry() {
        return new JobBeanDefinitionRegistry();
    }

    // ==================== 拦截器 ====================

    /**
     * 拦截器链（自动收集所有 JobExecutionInterceptor Bean）
     */
    @Bean
    @ConditionalOnMissingBean
    public JobExecutionInterceptorChain jobExecutionInterceptorChain(
            ObjectProvider<List<JobExecutionInterceptor>> interceptorsProvider) {
        List<JobExecutionInterceptor> interceptors = interceptorsProvider.getIfAvailable();
        return new JobExecutionInterceptorChain(interceptors);
    }

    @Bean
    @ConditionalOnMissingBean
    public Invoker invoker(JobHandlerRegistry jobRegistry, 
                          JobExecutionInterceptorChain interceptorChain) {
        return new DefaultInvoker(jobRegistry, interceptorChain);
    }

    // ==================== 日志发送 ====================

    @Bean
    @ConditionalOnMissingBean
    public JobLogSender jobLogSender() {
        return new JobLogSender();
    }

    // ==================== 核心组件 ====================

    @Bean
    @ConditionalOnMissingBean
    public ConnectorProcessor connectorProcessor(CloseableExecutor closeableExecutor,
                                                 SerializerHolder serializerHolder,
                                                 JobBeanDefinitionRegistry jobBeanDefinitionRegistry,
                                                 Invoker invoker,
                                                 JobLogSender jobLogSender) {
        return new DefaultClientProcessor(closeableExecutor, serializerHolder, 
                jobBeanDefinitionRegistry, invoker, properties, jobLogSender);
    }

    @Bean
    @ConditionalOnMissingBean
    public DefaultClient defaultClient(ConnectorProcessor connectorProcessor) {
        return new DefaultClient(properties, connectorProcessor);
    }
}
