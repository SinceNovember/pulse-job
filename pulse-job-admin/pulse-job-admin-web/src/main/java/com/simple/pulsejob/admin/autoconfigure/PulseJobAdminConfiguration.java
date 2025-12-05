package com.simple.pulsejob.admin.autoconfigure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import com.simple.plusejob.serialization.Serializer;
import com.simple.plusejob.serialization.SerializerType;
import com.simple.pulsejob.admin.scheduler.timer.HashedWheelTimer;
import com.simple.pulsejob.admin.scheduler.timer.Timer;
import com.simple.pulsejob.common.concurrent.JNamedThreadFactory;
import com.simple.pulsejob.serialization.hessian.HessianSerializer;
import com.simple.pulsejob.serialization.java.JavaSerializer;
import com.simple.pulsejob.transport.JAcceptor;
import com.simple.pulsejob.transport.netty.JNettyTcpAcceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(PulseJobAdminProperties.class)
@EnableScheduling
public class PulseJobAdminConfiguration {

    private final PulseJobAdminProperties properties;

    @Bean
    public JAcceptor defaultAcceptor() {
        return new JNettyTcpAcceptor(properties.getPort());
    }

    @Bean
    public List<Serializer> serializers() {
        return List.of(new JavaSerializer(), new HessianSerializer());
    }

    /**
     * 任务调度线程池
     * 用于执行定时任务的调度逻辑
     */
    @Bean("jobSchedulerExecutor")
    public ThreadPoolExecutor jobSchedulerExecutor() {
        return new ThreadPoolExecutor(
            properties.getThreadPool().getSchedulerCorePoolSize(),
            properties.getThreadPool().getSchedulerMaxPoolSize(),
            properties.getThreadPool().getKeepAliveSeconds(), TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(properties.getThreadPool().getSchedulerQueueSize()),
            new JNamedThreadFactory("job-scheduler-"),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * 任务执行线程池
     * 用于执行具体的任务逻辑
     */
    @Bean("jobExecutor")
    public ThreadPoolExecutor jobExecutor() {
        return new ThreadPoolExecutor(
            properties.getThreadPool().getExecutorCorePoolSize(),
            properties.getThreadPool().getExecutorMaxPoolSize(),
            properties.getThreadPool().getKeepAliveSeconds(), TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(properties.getThreadPool().getExecutorQueueSize()),
            new JNamedThreadFactory("job-executor-"),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * 通用异步处理线程池
     * 用于处理异步事件和回调
     */
    @Bean("asyncExecutor")
    public ThreadPoolExecutor asyncExecutor() {
        return new ThreadPoolExecutor(
            properties.getThreadPool().getAsyncCorePoolSize(),
            properties.getThreadPool().getAsyncMaxPoolSize(),
            properties.getThreadPool().getKeepAliveSeconds(), TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(properties.getThreadPool().getAsyncQueueSize()),
            new JNamedThreadFactory("async-"),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * 哈希时间轮定时器
     * 用于高效处理大量定时任务
     */
    @Bean
    public Timer hashedWheelTimer() {

        PulseJobAdminProperties.HashedWheelTimer config = properties.getHashedWheelTimer();

        // 解析时间单位
        TimeUnit timeUnit = TimeUnit.valueOf(config.getTickDurationUnit());

        // 创建线程工厂
        JNamedThreadFactory threadFactory = new JNamedThreadFactory(config.getThreadNamePrefix());

        // 创建时间轮定时器
        HashedWheelTimer timer = new HashedWheelTimer(
            threadFactory,
            config.getTickDuration(),
            timeUnit,
            config.getTicksPerWheel(),
            config.getMaxPendingTimeouts(),
            jobSchedulerExecutor() // 使用任务调度线程池作为执行器
        );

        // 启动时间轮
        timer.start();

        return timer;
    }

}
