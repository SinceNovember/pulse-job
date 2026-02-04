package com.simple.pulsejob.admin.websocket.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simple.pulsejob.admin.websocket.heartbeat.HeartbeatService;
import com.simple.pulsejob.admin.websocket.message.WebSocketMessage;
import com.simple.pulsejob.admin.websocket.session.WebSocketSessionManager;
import com.simple.pulsejob.admin.websocket.session.WebSocketSessionWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WebSocket 消息处理器
 * 
 * <p>处理所有 WebSocket 消息，包括：
 * <ul>
 *   <li>连接建立和关闭</li>
 *   <li>心跳消息</li>
 *   <li>订阅/取消订阅</li>
 *   <li>业务消息路由</li>
 * </ul>
 * 
 * @author PulseJob
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PulseJobWebSocketHandler extends TextWebSocketHandler implements org.springframework.beans.factory.DisposableBean {

    private final WebSocketSessionManager sessionManager;
    private final HeartbeatService heartbeatService;
    private final ObjectMapper objectMapper;
    
    // 应用关闭标志，避免关闭期间的错误日志
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    
    @Override
    public void destroy() {
        shuttingDown.set(true);
        log.info("WebSocket handler shutting down, suppressing transport errors");
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        WebSocketSessionWrapper wrapper = sessionManager.register(session);
        if (wrapper == null) {
            log.warn("Failed to register session, closing connection: {}", session.getId());
            session.close(CloseStatus.SERVICE_OVERLOAD);
            return;
        }

        // 发送连接成功消息
        Map<String, Object> connectInfo = new HashMap<>();
        connectInfo.put("sessionId", session.getId());
        connectInfo.put("clientId", wrapper.getClientId());
        connectInfo.put("clientType", wrapper.getClientType());
        connectInfo.put("serverTime", System.currentTimeMillis());

        WebSocketMessage<Map<String, Object>> connectMessage = WebSocketMessage.<Map<String, Object>>builder()
                .type(WebSocketMessage.MessageType.CONNECT)
                .data(connectInfo)
                .build();

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(connectMessage)));
        
        log.info("WebSocket connection established: sessionId={}, clientId={}, clientType={}", 
                session.getId(), wrapper.getClientId(), wrapper.getClientType());
        
        // 如果是执行器客户端连接，广播给所有浏览器客户端
        if ("executor".equals(wrapper.getClientType())) {
            broadcastExecutorOnline(wrapper);
        }
        
        // 广播当前连接统计给所有浏览器
        broadcastConnectionStats();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        WebSocketSessionWrapper wrapper = sessionManager.getSession(sessionId);
        String clientId = wrapper != null ? wrapper.getClientId() : "unknown";
        String clientType = wrapper != null ? wrapper.getClientType() : "unknown";
        
        // 应用关闭期间跳过广播，避免向已关闭的连接发送消息
        if (!shuttingDown.get()) {
            // 如果是执行器客户端断开，广播给所有浏览器客户端
            if (wrapper != null && "executor".equals(wrapper.getClientType())) {
                broadcastExecutorOffline(wrapper, status.getReason());
            }
        }
        
        sessionManager.unregister(sessionId);
        
        log.info("WebSocket connection closed: sessionId={}, clientId={}, clientType={}, status={}", 
                sessionId, clientId, clientType, status);
        
        // 应用关闭期间跳过广播
        if (!shuttingDown.get()) {
            // 广播当前连接统计给所有浏览器
            broadcastConnectionStats();
        }
    }
    
    /**
     * 广播执行器上线消息
     */
    private void broadcastExecutorOnline(WebSocketSessionWrapper wrapper) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("executorId", wrapper.getClientId());
            data.put("address", wrapper.getRemoteAddress());
            data.put("connectTime", wrapper.getConnectTime());
            data.put("event", "online");

            WebSocketMessage<Map<String, Object>> message = WebSocketMessage.<Map<String, Object>>builder()
                    .type(WebSocketMessage.MessageType.EXECUTOR_ONLINE)
                    .topic("executor.status")
                    .data(data)
                    .build();

            String json = objectMapper.writeValueAsString(message);
            sessionManager.broadcastToType("browser", json);
            
            log.info("Broadcast executor online: executorId={}, address={}", 
                    wrapper.getClientId(), wrapper.getRemoteAddress());
        } catch (Exception e) {
            log.error("Failed to broadcast executor online: {}", e.getMessage(), e);
        }
    }

    /**
     * 广播执行器下线消息
     */
    private void broadcastExecutorOffline(WebSocketSessionWrapper wrapper, String reason) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("executorId", wrapper.getClientId());
            data.put("address", wrapper.getRemoteAddress());
            data.put("disconnectTime", System.currentTimeMillis());
            data.put("reason", reason != null ? reason : "connection closed");
            data.put("event", "offline");

            WebSocketMessage<Map<String, Object>> message = WebSocketMessage.<Map<String, Object>>builder()
                    .type(WebSocketMessage.MessageType.EXECUTOR_OFFLINE)
                    .topic("executor.status")
                    .data(data)
                    .build();

            String json = objectMapper.writeValueAsString(message);
            sessionManager.broadcastToType("browser", json);
            
            log.info("Broadcast executor offline: executorId={}, reason={}", 
                    wrapper.getClientId(), reason);
        } catch (Exception e) {
            log.error("Failed to broadcast executor offline: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 广播连接统计信息
     */
    private void broadcastConnectionStats() {
        try {
            Map<String, Object> stats = sessionManager.getStatistics();
            stats.put("timestamp", System.currentTimeMillis());
            
            WebSocketMessage<Map<String, Object>> message = WebSocketMessage.<Map<String, Object>>builder()
                    .type(WebSocketMessage.MessageType.STATS_UPDATE)
                    .topic("connection.stats")
                    .data(stats)
                    .build();

            String json = objectMapper.writeValueAsString(message);
            sessionManager.broadcastToType("browser", json);
        } catch (Exception e) {
            log.error("Failed to broadcast connection stats: {}", e.getMessage(), e);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = session.getId();
        String payload = message.getPayload();

        try {
            JsonNode jsonNode = objectMapper.readTree(payload);
            String typeStr = jsonNode.has("type") ? jsonNode.get("type").asText() : null;

            if (typeStr == null) {
                sendError(session, "INVALID_MESSAGE", "Missing message type");
                return;
            }

            WebSocketMessage.MessageType type;
            try {
                type = WebSocketMessage.MessageType.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                sendError(session, "INVALID_TYPE", "Unknown message type: " + typeStr);
                return;
            }

            // 更新活动时间
            WebSocketSessionWrapper wrapper = sessionManager.getSession(sessionId);
            if (wrapper != null) {
                wrapper.updateActivity();
            }

            // 处理不同类型的消息
            switch (type) {
                case PING -> handlePing(session, jsonNode);
                case PONG -> handlePong(sessionId, jsonNode);
                case SUBSCRIBE -> handleSubscribe(session, jsonNode);
                case UNSUBSCRIBE -> handleUnsubscribe(session, jsonNode);
                default -> handleBusinessMessage(session, type, jsonNode);
            }

        } catch (JsonProcessingException e) {
            log.error("Failed to parse message from session {}: {}", sessionId, e.getMessage());
            sendError(session, "PARSE_ERROR", "Invalid JSON format");
        } catch (Exception e) {
            log.error("Error handling message from session {}: {}", sessionId, e.getMessage(), e);
            sendError(session, "INTERNAL_ERROR", "Internal server error");
        }
    }

    /**
     * 处理心跳请求
     */
    private void handlePing(WebSocketSession session, JsonNode jsonNode) throws Exception {
        long timestamp = jsonNode.has("data") ? jsonNode.get("data").asLong() : System.currentTimeMillis();
        
        WebSocketMessage<Long> pongMessage = WebSocketMessage.pong(timestamp);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(pongMessage)));
        
        // 更新心跳时间
        heartbeatService.refreshHeartbeat(session.getId());
    }

    /**
     * 处理心跳响应
     */
    private void handlePong(String sessionId, JsonNode jsonNode) {
        long timestamp = jsonNode.has("data") ? jsonNode.get("data").asLong() : 0;
        heartbeatService.handlePong(sessionId, timestamp);
    }

    /**
     * 处理订阅请求
     */
    private void handleSubscribe(WebSocketSession session, JsonNode jsonNode) throws Exception {
        String topic = jsonNode.has("topic") ? jsonNode.get("topic").asText() : null;
        if (topic == null || topic.isEmpty()) {
            sendError(session, "INVALID_TOPIC", "Topic is required for subscription");
            return;
        }

        sessionManager.subscribe(session.getId(), topic);

        WebSocketMessage<String> response = WebSocketMessage.subscribed(topic);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        
        log.debug("Session {} subscribed to topic: {}", session.getId(), topic);
    }

    /**
     * 处理取消订阅请求
     */
    private void handleUnsubscribe(WebSocketSession session, JsonNode jsonNode) throws Exception {
        String topic = jsonNode.has("topic") ? jsonNode.get("topic").asText() : null;
        if (topic == null || topic.isEmpty()) {
            sendError(session, "INVALID_TOPIC", "Topic is required for unsubscription");
            return;
        }

        sessionManager.unsubscribe(session.getId(), topic);

        WebSocketMessage<String> response = WebSocketMessage.<String>builder()
                .type(WebSocketMessage.MessageType.UNSUBSCRIBED)
                .topic(topic)
                .data(topic)
                .build();
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        
        log.debug("Session {} unsubscribed from topic: {}", session.getId(), topic);
    }

    /**
     * 处理业务消息
     */
    private void handleBusinessMessage(WebSocketSession session, WebSocketMessage.MessageType type, 
                                        JsonNode jsonNode) {
        String sessionId = session.getId();
        WebSocketSessionWrapper wrapper = sessionManager.getSession(sessionId);
        
        if (wrapper == null) {
            log.warn("Session not found for business message: {}", sessionId);
            return;
        }

        // 根据消息类型路由到不同的业务处理器
        switch (type) {
            case EXECUTOR_HEARTBEAT -> handleExecutorHeartbeat(wrapper, jsonNode);
            case EXECUTOR_STATUS -> handleExecutorStatusReport(wrapper, jsonNode);
            default -> log.debug("Received business message: type={}, sessionId={}, clientId={}", 
                    type, sessionId, wrapper.getClientId());
        }
    }
    
    /**
     * 处理执行器心跳上报
     */
    private void handleExecutorHeartbeat(WebSocketSessionWrapper wrapper, JsonNode jsonNode) {
        try {
            wrapper.updateHeartbeat();
            
            // 提取执行器状态信息
            Map<String, Object> heartbeatData = new HashMap<>();
            heartbeatData.put("executorId", wrapper.getClientId());
            heartbeatData.put("address", wrapper.getRemoteAddress());
            heartbeatData.put("timestamp", System.currentTimeMillis());
            
            // 提取额外的状态信息（如果有）
            if (jsonNode.has("data")) {
                JsonNode data = jsonNode.get("data");
                if (data.has("cpuUsage")) heartbeatData.put("cpuUsage", data.get("cpuUsage").asDouble());
                if (data.has("memoryUsage")) heartbeatData.put("memoryUsage", data.get("memoryUsage").asDouble());
                if (data.has("runningJobs")) heartbeatData.put("runningJobs", data.get("runningJobs").asInt());
                if (data.has("queueSize")) heartbeatData.put("queueSize", data.get("queueSize").asInt());
            }
            
            // 广播给浏览器客户端
            WebSocketMessage<Map<String, Object>> message = WebSocketMessage.<Map<String, Object>>builder()
                    .type(WebSocketMessage.MessageType.EXECUTOR_HEARTBEAT)
                    .topic("executor.status")
                    .data(heartbeatData)
                    .build();

            String json = objectMapper.writeValueAsString(message);
            sessionManager.broadcastToType("browser", json);
            
            log.trace("Executor heartbeat received: executorId={}", wrapper.getClientId());
        } catch (Exception e) {
            log.error("Failed to handle executor heartbeat: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 处理执行器状态上报
     */
    private void handleExecutorStatusReport(WebSocketSessionWrapper wrapper, JsonNode jsonNode) {
        try {
            Map<String, Object> statusData = new HashMap<>();
            statusData.put("executorId", wrapper.getClientId());
            statusData.put("address", wrapper.getRemoteAddress());
            statusData.put("status", "online");
            statusData.put("timestamp", System.currentTimeMillis());
            
            // 提取详细状态
            if (jsonNode.has("data")) {
                JsonNode data = jsonNode.get("data");
                if (data.has("cpuUsage")) statusData.put("cpuUsage", data.get("cpuUsage").asDouble());
                if (data.has("memoryUsage")) statusData.put("memoryUsage", data.get("memoryUsage").asDouble());
                if (data.has("diskUsage")) statusData.put("diskUsage", data.get("diskUsage").asDouble());
                if (data.has("runningJobs")) statusData.put("runningJobs", data.get("runningJobs").asInt());
                if (data.has("completedJobs")) statusData.put("completedJobs", data.get("completedJobs").asLong());
                if (data.has("failedJobs")) statusData.put("failedJobs", data.get("failedJobs").asLong());
                if (data.has("threadPoolSize")) statusData.put("threadPoolSize", data.get("threadPoolSize").asInt());
                if (data.has("activeThreads")) statusData.put("activeThreads", data.get("activeThreads").asInt());
            }
            
            // 广播给浏览器客户端
            WebSocketMessage<Map<String, Object>> message = WebSocketMessage.<Map<String, Object>>builder()
                    .type(WebSocketMessage.MessageType.EXECUTOR_STATUS)
                    .topic("executor.status")
                    .data(statusData)
                    .build();

            String json = objectMapper.writeValueAsString(message);
            sessionManager.broadcastToType("browser", json);
            
            log.debug("Executor status reported: executorId={}, data={}", wrapper.getClientId(), statusData);
        } catch (Exception e) {
            log.error("Failed to handle executor status report: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送错误消息
     */
    private void sendError(WebSocketSession session, String errorCode, String errorMessage) {
        try {
            WebSocketMessage<Void> error = WebSocketMessage.error(errorCode, errorMessage);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
        } catch (Exception e) {
            log.error("Failed to send error message: {}", e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        
        // 判断是否是预期的关闭错误（应用关闭或连接已关闭）
        boolean isExpectedCloseError = shuttingDown.get() 
                || isClosedChannelException(exception);
        
        if (isExpectedCloseError) {
            // 降级为 DEBUG 级别日志，避免关闭时的错误日志噪音
            log.debug("WebSocket transport closed: sessionId={}, reason={}", sessionId, exception.getMessage());
        } else {
            log.error("WebSocket transport error: sessionId={}, error={}", sessionId, exception.getMessage(), exception);
        }
        
        sessionManager.unregister(sessionId);
    }
    
    /**
     * 判断是否是 ClosedChannelException（可能被包装在 IOException 中）
     */
    private boolean isClosedChannelException(Throwable exception) {
        if (exception instanceof ClosedChannelException) {
            return true;
        }
        if (exception instanceof IOException) {
            Throwable cause = exception.getCause();
            return cause instanceof ClosedChannelException;
        }
        return false;
    }
}
