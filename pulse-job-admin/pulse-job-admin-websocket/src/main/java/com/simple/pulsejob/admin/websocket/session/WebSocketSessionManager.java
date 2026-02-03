package com.simple.pulsejob.admin.websocket.session;

import com.simple.pulsejob.admin.websocket.config.WebSocketProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * WebSocket 会话管理器
 * 
 * <p>管理所有 WebSocket 连接，提供：
 * <ul>
 *   <li>会话注册和注销</li>
 *   <li>按客户端ID/类型查询会话</li>
 *   <li>主题订阅管理</li>
 *   <li>消息广播和定向发送</li>
 *   <li>会话统计信息</li>
 * </ul>
 * 
 * @author PulseJob
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSessionManager {

    private final WebSocketProperties properties;

    /**
     * 所有会话映射：sessionId -> SessionWrapper
     */
    private final ConcurrentHashMap<String, WebSocketSessionWrapper> sessions = new ConcurrentHashMap<>();

    /**
     * 客户端ID到会话ID的映射（支持同一客户端多个连接）
     */
    private final ConcurrentHashMap<String, Set<String>> clientSessions = new ConcurrentHashMap<>();

    /**
     * 客户端类型到会话ID的映射
     */
    private final ConcurrentHashMap<String, Set<String>> typeSessions = new ConcurrentHashMap<>();

    /**
     * 主题订阅映射：topic -> sessionIds
     */
    private final ConcurrentHashMap<String, Set<String>> topicSubscriptions = new ConcurrentHashMap<>();

    /**
     * 注册会话
     */
    public WebSocketSessionWrapper register(WebSocketSession session) {
        if (sessions.size() >= properties.getMaxSessions()) {
            log.warn("Max sessions limit reached: {}", properties.getMaxSessions());
            return null;
        }

        String sessionId = session.getId();
        String clientId = (String) session.getAttributes().get("clientId");
        String clientType = (String) session.getAttributes().get("clientType");
        String remoteAddress = (String) session.getAttributes().get("remoteAddress");
        Long connectTime = (Long) session.getAttributes().get("connectTime");

        WebSocketSessionWrapper wrapper = WebSocketSessionWrapper.builder()
                .session(session)
                .clientId(clientId)
                .clientType(clientType != null ? clientType : "browser")
                .remoteAddress(remoteAddress)
                .connectTime(connectTime != null ? connectTime : System.currentTimeMillis())
                .lastHeartbeatTime(System.currentTimeMillis())
                .lastActiveTime(System.currentTimeMillis())
                .subscriptions(ConcurrentHashMap.newKeySet())
                .state(WebSocketSessionWrapper.SessionState.CONNECTED)
                .attributes(new ConcurrentHashMap<>())
                .build();

        sessions.put(sessionId, wrapper);

        // 添加到客户端映射
        if (clientId != null) {
            clientSessions.computeIfAbsent(clientId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        }

        // 添加到类型映射
        typeSessions.computeIfAbsent(wrapper.getClientType(), k -> ConcurrentHashMap.newKeySet()).add(sessionId);

        log.info("Session registered: sessionId={}, clientId={}, clientType={}, totalSessions={}", 
                sessionId, clientId, clientType, sessions.size());

        return wrapper;
    }

    /**
     * 注销会话
     */
    public void unregister(String sessionId) {
        WebSocketSessionWrapper wrapper = sessions.remove(sessionId);
        if (wrapper == null) {
            return;
        }

        wrapper.setState(WebSocketSessionWrapper.SessionState.CLOSED);

        // 从客户端映射中移除
        if (wrapper.getClientId() != null) {
            Set<String> sessionIds = clientSessions.get(wrapper.getClientId());
            if (sessionIds != null) {
                sessionIds.remove(sessionId);
                if (sessionIds.isEmpty()) {
                    clientSessions.remove(wrapper.getClientId());
                }
            }
        }

        // 从类型映射中移除
        Set<String> typeSessionIds = typeSessions.get(wrapper.getClientType());
        if (typeSessionIds != null) {
            typeSessionIds.remove(sessionId);
            if (typeSessionIds.isEmpty()) {
                typeSessions.remove(wrapper.getClientType());
            }
        }

        // 从所有订阅主题中移除
        for (String topic : wrapper.getSubscriptions()) {
            Set<String> subscribers = topicSubscriptions.get(topic);
            if (subscribers != null) {
                subscribers.remove(sessionId);
                if (subscribers.isEmpty()) {
                    topicSubscriptions.remove(topic);
                }
            }
        }

        log.info("Session unregistered: sessionId={}, clientId={}, totalSessions={}", 
                sessionId, wrapper.getClientId(), sessions.size());
    }

    /**
     * 获取会话
     */
    public WebSocketSessionWrapper getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * 根据客户端ID获取所有会话
     */
    public List<WebSocketSessionWrapper> getSessionsByClientId(String clientId) {
        Set<String> sessionIds = clientSessions.get(clientId);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Collections.emptyList();
        }
        return sessionIds.stream()
                .map(sessions::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 根据客户端类型获取所有会话
     */
    public List<WebSocketSessionWrapper> getSessionsByType(String clientType) {
        Set<String> sessionIds = typeSessions.get(clientType);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Collections.emptyList();
        }
        return sessionIds.stream()
                .map(sessions::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 订阅主题
     */
    public void subscribe(String sessionId, String topic) {
        WebSocketSessionWrapper wrapper = sessions.get(sessionId);
        if (wrapper != null) {
            wrapper.subscribe(topic);
            topicSubscriptions.computeIfAbsent(topic, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
            log.debug("Session {} subscribed to topic {}", sessionId, topic);
        }
    }

    /**
     * 取消订阅
     */
    public void unsubscribe(String sessionId, String topic) {
        WebSocketSessionWrapper wrapper = sessions.get(sessionId);
        if (wrapper != null) {
            wrapper.unsubscribe(topic);
            Set<String> subscribers = topicSubscriptions.get(topic);
            if (subscribers != null) {
                subscribers.remove(sessionId);
                if (subscribers.isEmpty()) {
                    topicSubscriptions.remove(topic);
                }
            }
            log.debug("Session {} unsubscribed from topic {}", sessionId, topic);
        }
    }

    /**
     * 向指定会话发送消息
     */
    public boolean sendMessage(String sessionId, String message) {
        WebSocketSessionWrapper wrapper = sessions.get(sessionId);
        if (wrapper == null || !wrapper.isValid()) {
            return false;
        }

        try {
            synchronized (wrapper.getSession()) {
                wrapper.getSession().sendMessage(new TextMessage(message));
            }
            wrapper.updateActivity();
            return true;
        } catch (IOException e) {
            log.error("Failed to send message to session {}: {}", sessionId, e.getMessage());
            return false;
        }
    }

    /**
     * 向客户端发送消息（所有连接）
     */
    public int sendToClient(String clientId, String message) {
        List<WebSocketSessionWrapper> wrappers = getSessionsByClientId(clientId);
        int successCount = 0;
        for (WebSocketSessionWrapper wrapper : wrappers) {
            if (sendMessage(wrapper.getSession().getId(), message)) {
                successCount++;
            }
        }
        return successCount;
    }

    /**
     * 向指定类型的客户端广播消息
     */
    public int broadcastToType(String clientType, String message) {
        List<WebSocketSessionWrapper> wrappers = getSessionsByType(clientType);
        int successCount = 0;
        for (WebSocketSessionWrapper wrapper : wrappers) {
            if (sendMessage(wrapper.getSession().getId(), message)) {
                successCount++;
            }
        }
        return successCount;
    }

    /**
     * 向订阅主题的会话广播消息
     */
    public int broadcastToTopic(String topic, String message) {
        Set<String> subscribers = topicSubscriptions.get(topic);
        if (subscribers == null || subscribers.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (String sessionId : subscribers) {
            if (sendMessage(sessionId, message)) {
                successCount++;
            }
        }
        return successCount;
    }

    /**
     * 广播消息到所有会话
     */
    public int broadcast(String message) {
        int successCount = 0;
        for (WebSocketSessionWrapper wrapper : sessions.values()) {
            if (sendMessage(wrapper.getSession().getId(), message)) {
                successCount++;
            }
        }
        return successCount;
    }

    /**
     * 获取所有会话
     */
    public Collection<WebSocketSessionWrapper> getAllSessions() {
        return Collections.unmodifiableCollection(sessions.values());
    }

    /**
     * 获取会话数量
     */
    public int getSessionCount() {
        return sessions.size();
    }

    /**
     * 获取指定类型的会话数量
     */
    public int getSessionCountByType(String clientType) {
        Set<String> sessionIds = typeSessions.get(clientType);
        return sessionIds != null ? sessionIds.size() : 0;
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSessions", sessions.size());
        stats.put("maxSessions", properties.getMaxSessions());
        stats.put("browserSessions", getSessionCountByType("browser"));
        stats.put("executorSessions", getSessionCountByType("executor"));
        stats.put("totalTopics", topicSubscriptions.size());
        stats.put("totalClients", clientSessions.size());
        return stats;
    }

    /**
     * 检查会话是否存在且有效
     */
    public boolean isSessionValid(String sessionId) {
        WebSocketSessionWrapper wrapper = sessions.get(sessionId);
        return wrapper != null && wrapper.isValid();
    }
}
