package com.simple.pulsejob.admin.websocket.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket 消息统一格式
 * 
 * @author PulseJob
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebSocketMessage<T> {

    /**
     * 消息类型
     */
    private MessageType type;

    /**
     * 消息主题（用于订阅分发）
     */
    private String topic;

    /**
     * 消息ID
     */
    private String messageId;

    /**
     * 消息数据
     */
    private T data;

    /**
     * 时间戳
     */
    @Builder.Default
    private long timestamp = System.currentTimeMillis();

    /**
     * 错误码（错误消息时使用）
     */
    private String errorCode;

    /**
     * 错误消息
     */
    private String errorMessage;

    /**
     * 消息类型枚举
     */
    public enum MessageType {
        // 系统消息
        PING,           // 心跳请求
        PONG,           // 心跳响应
        CONNECT,        // 连接成功
        DISCONNECT,     // 断开连接
        ERROR,          // 错误消息
        ACK,            // 确认消息

        // 订阅消息
        SUBSCRIBE,      // 订阅主题
        UNSUBSCRIBE,    // 取消订阅
        SUBSCRIBED,     // 订阅成功
        UNSUBSCRIBED,   // 取消订阅成功

        // 业务消息 - 执行器状态
        EXECUTOR_STATUS,        // 执行器状态更新
        EXECUTOR_ONLINE,        // 执行器上线
        EXECUTOR_OFFLINE,       // 执行器下线
        EXECUTOR_HEARTBEAT,     // 执行器心跳

        // 业务消息 - 任务相关
        TASK_STATUS,            // 任务状态变更
        TASK_TRIGGERED,         // 任务触发
        TASK_COMPLETED,         // 任务完成
        TASK_FAILED,            // 任务失败

        // 业务消息 - 日志相关
        LOG_STREAM,             // 日志流
        LOG_APPEND,             // 日志追加
        LOG_END,                // 日志结束

        // 业务消息 - 告警相关
        ALERT,                  // 告警通知

        // 业务消息 - 统计相关
        STATS_UPDATE,           // 统计数据更新

        // 通用消息
        BROADCAST,              // 广播消息
        NOTIFICATION            // 通知消息
    }

    /**
     * 创建成功响应
     */
    public static <T> WebSocketMessage<T> success(MessageType type, T data) {
        return WebSocketMessage.<T>builder()
                .type(type)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建错误响应
     */
    public static WebSocketMessage<Void> error(String errorCode, String errorMessage) {
        return WebSocketMessage.<Void>builder()
                .type(MessageType.ERROR)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建心跳消息
     */
    public static WebSocketMessage<Long> ping() {
        return WebSocketMessage.<Long>builder()
                .type(MessageType.PING)
                .data(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建心跳响应
     */
    public static WebSocketMessage<Long> pong(long pingTimestamp) {
        return WebSocketMessage.<Long>builder()
                .type(MessageType.PONG)
                .data(pingTimestamp)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 创建订阅成功消息
     */
    public static WebSocketMessage<String> subscribed(String topic) {
        return WebSocketMessage.<String>builder()
                .type(MessageType.SUBSCRIBED)
                .topic(topic)
                .data(topic)
                .build();
    }

    /**
     * 创建通知消息
     */
    public static <T> WebSocketMessage<T> notification(String topic, T data) {
        return WebSocketMessage.<T>builder()
                .type(MessageType.NOTIFICATION)
                .topic(topic)
                .data(data)
                .build();
    }
}
