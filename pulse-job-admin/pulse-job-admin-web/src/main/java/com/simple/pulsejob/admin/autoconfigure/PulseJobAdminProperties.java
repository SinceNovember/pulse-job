package com.simple.pulsejob.admin.autoconfigure;

import com.simple.pulsejob.common.JConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = PulseJobAdminProperties.PULSE_JOB_PREFIX)
public class PulseJobAdminProperties {

    public static final String PULSE_JOB_PREFIX = "pulse.job.admin";

    private String host;

    private Integer port = 10410;

    /**
     * 线程池配置
     */
    private ThreadPool threadPool = new ThreadPool();

    /**
     * 时间轮配置
     */
    private HashedWheelTimer hashedWheelTimer = new HashedWheelTimer();

    @Data
    public static class ThreadPool {
        
        /**
         * 任务调度线程池核心线程数
         */
        private int schedulerCorePoolSize = JConstants.DEFAULT_CORE_POOL_SIZE;
        
        /**
         * 任务调度线程池最大线程数
         */
        private int schedulerMaxPoolSize = JConstants.DEFAULT_MAX_POOL_SIZE;
        
        /**
         * 任务调度线程池队列大小
         */
        private int schedulerQueueSize = 1000;
        
        /**
         * 任务执行线程池核心线程数
         */
        private int executorCorePoolSize = JConstants.AVAILABLE_PROCESSORS * 2;
        
        /**
         * 任务执行线程池最大线程数
         */
        private int executorMaxPoolSize = JConstants.AVAILABLE_PROCESSORS * 4;
        
        /**
         * 任务执行线程池队列大小
         */
        private int executorQueueSize = 2000;
        
        /**
         * 异步处理线程池核心线程数
         */
        private int asyncCorePoolSize = JConstants.AVAILABLE_PROCESSORS;
        
        /**
         * 异步处理线程池最大线程数
         */
        private int asyncMaxPoolSize = JConstants.AVAILABLE_PROCESSORS * 2;
        
        /**
         * 异步处理线程池队列大小
         */
        private int asyncQueueSize = 500;
        
        /**
         * 线程空闲时间（秒）
         */
        private long keepAliveSeconds = 60L;
    }

    @Data
    public static class HashedWheelTimer {
        
        /**
         * 时间轮刻度间隔（毫秒）
         */
        private long tickDuration = 1L;
        
        /**
         * 时间轮刻度间隔时间单位
         */
        private String tickDurationUnit = "SECONDS";
        
        /**
         * 时间轮刻度数量
         */
        private int ticksPerWheel = 512;
        
        /**
         * 最大等待任务数，-1表示无限制
         */
        private long maxPendingTimeouts = -1L;
        
        /**
         * 线程工厂名称前缀
         */
        private String threadNamePrefix = "hashed-wheel-timer-";
    }
}
