package com.simple.pulsejob.admin.controller;

import com.simple.pulsejob.admin.common.model.base.ResponseResult;
import com.simple.pulsejob.admin.websocket.heartbeat.HeartbeatService;
import com.simple.pulsejob.admin.websocket.session.WebSocketSessionManager;
import com.simple.pulsejob.admin.websocket.session.WebSocketSessionWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
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
@Slf4j
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
    public ResponseResult<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>(sessionManager.getStatistics());
        stats.put("heartbeatServiceRunning", heartbeatService.isRunning());
        return ResponseResult.ok(stats);
    }

    /**
     * 获取所有会话列表
     */
    @GetMapping("/sessions")
    public ResponseResult<List<Map<String, Object>>> getSessions(
            @RequestParam(required = false) String clientType,
            @RequestParam(required = false) String clientId) {
        
        List<Map<String, Object>> sessions;
        
        if (clientType != null && !clientType.isEmpty()) {
            sessions = sessionManager.getSessionsByType(clientType).stream()
                    .map(this::toSessionInfo)
                    .toList();
        } else if (clientId != null && !clientId.isEmpty()) {
            sessions = sessionManager.getSessionsByClientId(clientId).stream()
                    .map(this::toSessionInfo)
                    .toList();
        } else {
            sessions = sessionManager.getAllSessions().stream()
                    .map(this::toSessionInfo)
                    .toList();
        }
        
        return ResponseResult.ok(sessions);
    }

    /**
     * 检查会话是否有效
     */
    @GetMapping("/sessions/{sessionId}/valid")
    public ResponseResult<Map<String, Boolean>> checkSessionValid(@PathVariable("sessionId") String sessionId) {
        boolean valid = sessionManager.isSessionValid(sessionId);
        return ResponseResult.ok(Map.of("valid", valid));
    }

    /**
     * 关闭指定会话
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseResult<Void> closeSession(@PathVariable("sessionId") String sessionId) {
        try {
            var wrapper = sessionManager.getSession(sessionId);
            if (wrapper != null && wrapper.getSession().isOpen()) {
                wrapper.getSession().close();
            }
            sessionManager.unregister(sessionId);
            return ResponseResult.ok();
        } catch (Exception e) {
            log.error("关闭会话失败: {}", sessionId, e);
            return ResponseResult.error("关闭会话失败: " + e.getMessage());
        }
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
