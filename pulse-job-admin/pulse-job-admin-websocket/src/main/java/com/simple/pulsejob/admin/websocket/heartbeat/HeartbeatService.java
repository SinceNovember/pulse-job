package com.simple.pulsejob.admin.websocket.heartbeat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simple.pulsejob.admin.websocket.config.WebSocketProperties;
import com.simple.pulsejob.admin.websocket.message.WebSocketMessage;
import com.simple.pulsejob.admin.websocket.session.WebSocketSessionManager;
import com.simple.pulsejob.admin.websocket.session.WebSocketSessionWrapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 心跳检测服务
 * 
 * <p>定期检测 WebSocket 连接状态，清理超时连接
 * 
 * <p>功能：
 * <ul>
 *   <li>定期发送心跳探测</li>
 *   <li>检测并关闭超时连接</li>
 *   <li>检测并关闭空闲连接</li>
 *   <li>统计心跳健康状态</li>
 * </ul>
 * 
 * @author PulseJob
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HeartbeatService {

    private final WebSocketSessionManager sessionManager;
    private final WebSocketProperties properties;
    private final ObjectMapper objectMapper;

    private ScheduledExecutorService scheduler;
    private volatile boolean running = false;

    @PostConstruct
    public void init() {
        if (!properties.isEnabled()) {
            log.info("WebSocket is disabled, heartbeat service will not start");
            return;
        }
        start();
    }

    @PreDestroy
    public void destroy() {
        stop();
    }

    /**
     * 启动心跳检测服务
     */
    public void start() {
        if (running) {
            log.warn("Heartbeat service is already running");
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "ws-heartbeat");
            thread.setDaemon(true);
            return thread;
        });

        // 定期执行心跳检测
        long interval = properties.getHeartbeatInterval();
        scheduler.scheduleAtFixedRate(this::checkHeartbeat, interval, interval, TimeUnit.MILLISECONDS);

        running = true;
        log.info("Heartbeat service started, interval={}ms, timeout={}ms", 
                interval, properties.getHeartbeatTimeout());
    }

    /**
     * 停止心跳检测服务
     */
    public void stop() {
        running = false;
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.info("Heartbeat service stopped");
    }

    /**
     * 检测心跳
     */
    private void checkHeartbeat() {
        if (!running) {
            return;
        }

        long now = System.currentTimeMillis();
        long heartbeatTimeout = properties.getHeartbeatTimeout();
        long idleTimeout = properties.getSessionIdleTimeout();
        
        List<String> sessionsToClose = new ArrayList<>();
        int heartbeatSent = 0;
        int timeoutCount = 0;
        int idleCount = 0;

        for (WebSocketSessionWrapper wrapper : sessionManager.getAllSessions()) {
            if (!wrapper.isValid()) {
                sessionsToClose.add(wrapper.getSession().getId());
                continue;
            }

            // 检查心跳超时
            if (now - wrapper.getLastHeartbeatTime() > heartbeatTimeout) {
                log.warn("Session heartbeat timeout: sessionId={}, clientId={}, lastHeartbeat={}ms ago",
                        wrapper.getSession().getId(), 
                        wrapper.getClientId(),
                        now - wrapper.getLastHeartbeatTime());
                sessionsToClose.add(wrapper.getSession().getId());
                timeoutCount++;
                continue;
            }

            // 检查空闲超时
            if (now - wrapper.getLastActiveTime() > idleTimeout) {
                log.info("Session idle timeout: sessionId={}, clientId={}, lastActive={}ms ago",
                        wrapper.getSession().getId(),
                        wrapper.getClientId(),
                        now - wrapper.getLastActiveTime());
                sessionsToClose.add(wrapper.getSession().getId());
                idleCount++;
                continue;
            }

            // 发送心跳探测
            try {
                sendHeartbeatPing(wrapper);
                heartbeatSent++;
            } catch (Exception e) {
                log.error("Failed to send heartbeat to session {}: {}", 
                        wrapper.getSession().getId(), e.getMessage());
            }
        }

        // 关闭超时/空闲会话
        for (String sessionId : sessionsToClose) {
            closeSession(sessionId);
        }

        if (timeoutCount > 0 || idleCount > 0 || heartbeatSent > 0) {
            log.debug("Heartbeat check completed: sent={}, timeout={}, idle={}, closed={}",
                    heartbeatSent, timeoutCount, idleCount, sessionsToClose.size());
        }
    }

    /**
     * 发送心跳 ping 消息
     */
    private void sendHeartbeatPing(WebSocketSessionWrapper wrapper) throws Exception {
        WebSocketMessage<Long> pingMessage = WebSocketMessage.<Long>builder()
                .type(WebSocketMessage.MessageType.PING)
                .data(System.currentTimeMillis())
                .build();

        String json = objectMapper.writeValueAsString(pingMessage);
        sessionManager.sendMessage(wrapper.getSession().getId(), json);
    }

    /**
     * 处理心跳响应
     */
    public void handlePong(String sessionId, long timestamp) {
        WebSocketSessionWrapper wrapper = sessionManager.getSession(sessionId);
        if (wrapper != null) {
            wrapper.updateHeartbeat();
            long latency = System.currentTimeMillis() - timestamp;
            log.debug("Heartbeat pong received: sessionId={}, latency={}ms", sessionId, latency);
        }
    }

    /**
     * 关闭会话
     */
    private void closeSession(String sessionId) {
        WebSocketSessionWrapper wrapper = sessionManager.getSession(sessionId);
        if (wrapper != null && wrapper.getSession().isOpen()) {
            try {
                wrapper.getSession().close();
            } catch (IOException e) {
                log.error("Failed to close session {}: {}", sessionId, e.getMessage());
            }
        }
        sessionManager.unregister(sessionId);
    }

    /**
     * 手动触发心跳更新
     */
    public void refreshHeartbeat(String sessionId) {
        WebSocketSessionWrapper wrapper = sessionManager.getSession(sessionId);
        if (wrapper != null) {
            wrapper.updateHeartbeat();  
        }
    }

    /**
     * 获取服务状态
     */
    public boolean isRunning() {
        return running;
    }
}
