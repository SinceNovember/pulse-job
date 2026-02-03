package com.simple.pulsejob.admin.websocket.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * WebSocket 配置属性
 * 
 * @author PulseJob
 */
@Data
@Component
@ConfigurationProperties(prefix = "pulse.job.admin.websocket")
public class WebSocketProperties {

    /**
     * 是否启用 WebSocket
     */
    private boolean enabled = true;

    /**
     * 心跳间隔（毫秒）
     */
    private long heartbeatInterval = 30000;

    /**
     * 心跳超时时间（毫秒）
     * 超过此时间未收到心跳则认为连接断开
     */
    private long heartbeatTimeout = 90000;

    /**
     * 最大会话数
     */
    private int maxSessions = 1000;

    /**
     * 消息发送超时（毫秒）
     */
    private long sendTimeout = 10000;

    /**
     * 消息缓冲区大小（字节）
     */
    private int bufferSize = 8192;

    /**
     * 最大文本消息大小（字节）
     */
    private int maxTextMessageSize = 65536;

    /**
     * 是否启用消息压缩
     */
    private boolean compressionEnabled = true;

    /**
     * 会话空闲超时时间（毫秒）
     * 超过此时间无活动则关闭连接
     */
    private long sessionIdleTimeout = 300000;
}
