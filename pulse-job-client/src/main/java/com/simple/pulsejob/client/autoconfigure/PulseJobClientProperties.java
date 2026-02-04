package com.simple.pulsejob.client.autoconfigure;

import com.simple.pulsejob.common.JConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = PulseJobClientProperties.PULSE_JOB_PREFIX)
public class PulseJobClientProperties {

    public static final String PULSE_JOB_PREFIX = "pulse.job";

    /**
     * 线程池类型（cached/fixed 等）
     */
    private String threadPool;

    private int corePoolSize = JConstants.DEFAULT_CORE_POOL_SIZE;

    private int workQueue = 1000;

    private int maxPoolSize = JConstants.DEFAULT_MAX_POOL_SIZE;

    private String serializerType;

    /**
     * 执行器配置
     */
    private Executor executor = new Executor();

    /**
     * 管理端配置
     */
    private Admin admin = new Admin();

    @Data
    public static class Executor {
        /**
         * 执行器名称（必填）
         */
        private String name;

        /**
         * 执行器 IP 地址（可选）
         * 不配置则自动获取本机 IP（连接管理端时的出站 IP）
         */
        private String ip;

        /**
         * 执行器端口（可选）
         * 不配置则使用连接的本地端口
         * 建议配置固定端口，避免重连时地址变化
         */
        private Integer port;
    }

    @Data
    public static class Admin {
        /**
         * 管理端地址
         */
        private String host;

        /**
         * 管理端端口
         */
        private Integer port = 10416;
    }
}
