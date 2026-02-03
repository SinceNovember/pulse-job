package com.simple.pulsejob.admin.websocket.session;

import lombok.Data;
import lombok.Builder;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 会话包装类
 * 
 * <p>封装原生 WebSocket 会话，添加业务相关属性
 * 
 * @author PulseJob
 */
@Data
@Builder
public class WebSocketSessionWrapper {

    /**
     * 原生 WebSocket 会话
     */
    private WebSocketSession session;

    /**
     * 客户端ID
     */
    private String clientId;

    /**
     * 客户端类型：browser（浏览器）、executor（执行器）
     */
    private String clientType;

    /**
     * 远程地址
     */
    private String remoteAddress;

    /**
     * 连接时间
     */
    private long connectTime;

    /**
     * 最后心跳时间
     */
    private volatile long lastHeartbeatTime;

    /**
     * 最后活动时间
     */
    private volatile long lastActiveTime;

    /**
     * 订阅的主题集合
     */
    private Set<String> subscriptions;

    /**
     * 会话状态
     */
    private volatile SessionState state;

    /**
     * 扩展属性
     */
    private ConcurrentHashMap<String, Object> attributes;

    /**
     * 会话状态枚举
     */
    public enum SessionState {
        CONNECTING,
        CONNECTED,
        CLOSING,
        CLOSED
    }

    /**
     * 更新心跳时间
     */
    public void updateHeartbeat() {
        this.lastHeartbeatTime = System.currentTimeMillis();
        this.lastActiveTime = System.currentTimeMillis();
    }

    /**
     * 更新活动时间
     */
    public void updateActivity() {
        this.lastActiveTime = System.currentTimeMillis();
    }

    /**
     * 添加订阅
     */
    public void subscribe(String topic) {
        if (subscriptions != null) {
            subscriptions.add(topic);
        }
    }

    /**
     * 取消订阅
     */
    public void unsubscribe(String topic) {
        if (subscriptions != null) {
            subscriptions.remove(topic);
        }
    }

    /**
     * 检查是否已订阅
     */
    public boolean isSubscribed(String topic) {
        return subscriptions != null && subscriptions.contains(topic);
    }

    /**
     * 设置扩展属性
     */
    public void setAttribute(String key, Object value) {
        if (attributes == null) {
            attributes = new ConcurrentHashMap<>();
        }
        attributes.put(key, value);
    }

    /**
     * 获取扩展属性
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        if (attributes == null) {
            return null;
        }
        return (T) attributes.get(key);
    }

    /**
     * 检查连接是否有效
     */
    public boolean isValid() {
        return session != null && session.isOpen() && state == SessionState.CONNECTED;
    }
}
