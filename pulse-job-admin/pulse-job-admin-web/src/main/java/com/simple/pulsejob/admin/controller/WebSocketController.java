package com.simple.pulsejob.admin.controller;

import com.simple.pulsejob.admin.websocket.heartbeat.HeartbeatService;
import com.simple.pulsejob.admin.websocket.session.WebSocketSessionManager;
import com.simple.pulsejob.admin.websocket.session.WebSocketSessionWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * WebSocket 管理控制器
 * 
 * <p>提供 WebSocket 相关的 HTTP 接口，用于：
 * <ul>
 *   <li>查询连接状态</li>
 *   <li>管理会话</li>
 *   <li>获取统计信息</li>
 * </ul>
 * 
 * @author PulseJob
 */
@RestController
@RequestMapping("/api/websocket")
@RequiredArgsConstructor
public class WebSocketController {

    private final WebSocketSessionManager sessionManager;
    private final HeartbeatService heartbeatService;

    /**
     * 获取 WebSocket 统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = sessionManager.getStatistics();
        stats.put("heartbeatServiceRunning", heartbeatService.isRunning());
        return ResponseEntity.ok(stats);
    }

    /**
     * 获取所有会话列表
     */
    @GetMapping("/sessions")
    public ResponseEntity<?> getSessions(
            @RequestParam(required = false) String clientType,
            @RequestParam(required = false) String clientId) {
        
        if (clientType != null && !clientType.isEmpty()) {
            return ResponseEntity.ok(sessionManager.getSessionsByType(clientType).stream()
                    .map(this::toSessionInfo)
                    .toList());
        }
        
        if (clientId != null && !clientId.isEmpty()) {
            return ResponseEntity.ok(sessionManager.getSessionsByClientId(clientId).stream()
                    .map(this::toSessionInfo)
                    .toList());
        }

        return ResponseEntity.ok(sessionManager.getAllSessions().stream()
                .map(this::toSessionInfo)
                .toList());
    }

    /**
     * 检查会话是否有效
     */
    @GetMapping("/sessions/{sessionId}/valid")
    public ResponseEntity<Map<String, Boolean>> checkSessionValid(@PathVariable String sessionId) {
        boolean valid = sessionManager.isSessionValid(sessionId);
        return ResponseEntity.ok(Map.of("valid", valid));
    }

    /**
     * 关闭指定会话
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> closeSession(@PathVariable String sessionId) {
        var wrapper = sessionManager.getSession(sessionId);
        if (wrapper != null && wrapper.getSession().isOpen()) {
            try {
                wrapper.getSession().close();
            } catch (Exception e) {
                // ignore
            }
        }
        sessionManager.unregister(sessionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 转换会话信息为 Map
     */
    private Map<String, Object> toSessionInfo(WebSocketSessionWrapper wrapper) {
        return Map.of(
                "sessionId", wrapper.getSession().getId(),
                "clientId", wrapper.getClientId() != null ? wrapper.getClientId() : "",
                "clientType", wrapper.getClientType(),
                "remoteAddress", wrapper.getRemoteAddress() != null ? wrapper.getRemoteAddress() : "",
                "connectTime", wrapper.getConnectTime(),
                "lastHeartbeat", wrapper.getLastHeartbeatTime(),
                "lastActivity", wrapper.getLastActiveTime(),
                "state", wrapper.getState().name(),
                "subscriptions", wrapper.getSubscriptions()
        );
    }
}
