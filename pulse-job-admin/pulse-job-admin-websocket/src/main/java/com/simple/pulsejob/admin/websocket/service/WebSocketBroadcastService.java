package com.simple.pulsejob.admin.websocket.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simple.pulsejob.admin.websocket.message.WebSocketMessage;
import com.simple.pulsejob.admin.websocket.session.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * WebSocket 消息广播服务
 * 
 * <p>提供统一的消息推送接口，支持：
 * <ul>
 *   <li>执行器状态推送</li>
 *   <li>任务日志实时推送</li>
 *   <li>任务状态变更通知</li>
 *   <li>系统告警通知</li>
 *   <li>统计数据更新</li>
 * </ul>
 * 
 * @author PulseJob
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketBroadcastService {

    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    // ==================== 执行器状态相关 ====================

    /**
     * 推送执行器状态更新
     */
    @Async
    public void pushExecutorStatus(ExecutorStatusDTO status) {
        try {
            WebSocketMessage<ExecutorStatusDTO> message = WebSocketMessage.<ExecutorStatusDTO>builder()
                    .type(WebSocketMessage.MessageType.EXECUTOR_STATUS)
                    .topic("executor.status")
                    .data(status)
                    .build();

            String json = objectMapper.writeValueAsString(message);
            int count = sessionManager.broadcastToTopic("executor.status", json);
            
            // 同时广播给所有浏览器客户端
            sessionManager.broadcastToType("browser", json);
            
            log.debug("Executor status pushed: executorId={}, status={}, recipients={}", 
                    status.getExecutorId(), status.getStatus(), count);
        } catch (Exception e) {
            log.error("Failed to push executor status: {}", e.getMessage(), e);
        }
    }

    /**
     * 推送执行器上线通知
     */
    @Async
    public void pushExecutorOnline(String executorId, String address) {
        try {
            Map<String, String> data = Map.of(
                    "executorId", executorId,
                    "address", address,
                    "event", "online"
            );

            WebSocketMessage<Map<String, String>> message = WebSocketMessage.<Map<String, String>>builder()
                    .type(WebSocketMessage.MessageType.EXECUTOR_ONLINE)
                    .topic("executor.status")
                    .data(data)
                    .build();

            String json = objectMapper.writeValueAsString(message);
            sessionManager.broadcastToType("browser", json);
            
            log.info("Executor online notification pushed: executorId={}, address={}", executorId, address);
        } catch (Exception e) {
            log.error("Failed to push executor online: {}", e.getMessage(), e);
        }
    }

    /**
     * 推送执行器下线通知
     */
    @Async
    public void pushExecutorOffline(String executorId, String address, String reason) {
        try {
            Map<String, String> data = new java.util.HashMap<>();
            data.put("executorId", executorId);
            data.put("address", address != null ? address : "");
            data.put("reason", reason != null ? reason : "unknown");
            data.put("event", "offline");

            WebSocketMessage<Map<String, String>> message = WebSocketMessage.<Map<String, String>>builder()
                    .type(WebSocketMessage.MessageType.EXECUTOR_OFFLINE)
                    .topic("executor.status")
                    .data(data)
                    .build();

            String json = objectMapper.writeValueAsString(message);
            sessionManager.broadcastToType("browser", json);
            
            log.info("Executor offline notification pushed: executorId={}, address={}, reason={}", executorId, address, reason);
        } catch (Exception e) {
            log.error("Failed to push executor offline: {}", e.getMessage(), e);
        }
    }

    // ==================== 任务日志相关 ====================

    /**
     * 推送任务日志
     */
    @Async
    public void pushJobLog(Long jobId, Long instanceId, String logLine) {
        try {
            String topic = "job.log." + instanceId;
            
            JobLogDTO logData = JobLogDTO.builder()
                    .jobId(jobId)
                    .instanceId(instanceId)
                    .content(logLine)
                    .timestamp(System.currentTimeMillis())
                    .build();

            WebSocketMessage<JobLogDTO> message = WebSocketMessage.<JobLogDTO>builder()
                    .type(WebSocketMessage.MessageType.LOG_APPEND)
                    .topic(topic)
                    .data(logData)
                    .build();

            String json = objectMapper.writeValueAsString(message);
            int count = sessionManager.broadcastToTopic(topic, json);
            
            log.trace("Job log pushed: instanceId={}, recipients={}", instanceId, count);
        } catch (Exception e) {
            log.error("Failed to push job log: {}", e.getMessage(), e);
        }
    }

    /**
     * 推送日志流开始
     */
    @Async
    public void pushLogStreamStart(Long instanceId) {
        try {
            String topic = "job.log." + instanceId;
            
            WebSocketMessage<Long> message = WebSocketMessage.<Long>builder()
                    .type(WebSocketMessage.MessageType.LOG_STREAM)
                    .topic(topic)
                    .data(instanceId)
                    .build();

            String json = objectMapper.writeValueAsString(message);
            sessionManager.broadcastToTopic(topic, json);
        } catch (Exception e) {
            log.error("Failed to push log stream start: {}", e.getMessage(), e);
        }
    }

    /**
     * 推送日志流结束
     */
    @Async
    public void pushLogStreamEnd(Long instanceId, boolean success) {
        try {
            String topic = "job.log." + instanceId;
            
            Map<String, Object> data = Map.of(
                    "instanceId", instanceId,
                    "success", success,
                    "endTime", System.currentTimeMillis()
            );

            WebSocketMessage<Map<String, Object>> message = WebSocketMessage.<Map<String, Object>>builder()
                    .type(WebSocketMessage.MessageType.LOG_END)
                    .topic(topic)
                    .data(data)
                    .build();

            String json = objectMapper.writeValueAsString(message);
            sessionManager.broadcastToTopic(topic, json);
            
            log.debug("Log stream end pushed: instanceId={}, success={}", instanceId, success);
        } catch (Exception e) {
            log.error("Failed to push log stream end: {}", e.getMessage(), e);
        }
    }

    // ==================== 任务状态相关 ====================

    /**
     * 推送任务状态变更
     */
    @Async
    public void pushTaskStatus(Long jobId, String status, String message) {
        try {
            Map<String, Object> data = Map.of(
                    "jobId", jobId,
                    "status", status,
                    "message", message != null ? message : "",
                    "timestamp", System.currentTimeMillis()
            );

            WebSocketMessage<Map<String, Object>> wsMessage = WebSocketMessage.<Map<String, Object>>builder()
                    .type(WebSocketMessage.MessageType.TASK_STATUS)
                    .topic("task.status")
                    .data(data)
                    .build();

            String json = objectMapper.writeValueAsString(wsMessage);
            sessionManager.broadcastToTopic("task.status", json);
            sessionManager.broadcastToType("browser", json);
            
            log.debug("Task status pushed: jobId={}, status={}", jobId, status);
        } catch (Exception e) {
            log.error("Failed to push task status: {}", e.getMessage(), e);
        }
    }

    /**
     * 推送任务触发通知
     */
    @Async
    public void pushTaskTriggered(Long jobId, Long instanceId) {
        try {
            Map<String, Object> data = Map.of(
                    "jobId", jobId,
                    "instanceId", instanceId,
                    "triggerTime", System.currentTimeMillis()
            );

            WebSocketMessage<Map<String, Object>> message = WebSocketMessage.<Map<String, Object>>builder()
                    .type(WebSocketMessage.MessageType.TASK_TRIGGERED)
                    .topic("task.status")
                    .data(data)
                    .build();

            String json = objectMapper.writeValueAsString(message);
            sessionManager.broadcastToType("browser", json);
            
            log.debug("Task triggered pushed: jobId={}, instanceId={}", jobId, instanceId);
        } catch (Exception e) {
            log.error("Failed to push task triggered: {}", e.getMessage(), e);
        }
    }

    /**
     * 推送任务完成通知
     */
    @Async
    public void pushTaskCompleted(Long jobId, Long instanceId, boolean success, String result) {
        try {
            Map<String, Object> data = Map.of(
                    "jobId", jobId,
                    "instanceId", instanceId,
                    "success", success,
                    "result", result != null ? result : "",
                    "completeTime", System.currentTimeMillis()
            );

            WebSocketMessage.MessageType type = success ? 
                    WebSocketMessage.MessageType.TASK_COMPLETED : 
                    WebSocketMessage.MessageType.TASK_FAILED;

            WebSocketMessage<Map<String, Object>> message = WebSocketMessage.<Map<String, Object>>builder()
                    .type(type)
                    .topic("task.status")
                    .data(data)
                    .build();

            String json = objectMapper.writeValueAsString(message);
            sessionManager.broadcastToType("browser", json);
            
            log.debug("Task completed pushed: jobId={}, instanceId={}, success={}", jobId, instanceId, success);
        } catch (Exception e) {
            log.error("Failed to push task completed: {}", e.getMessage(), e);
        }
    }

    // ==================== 告警相关 ====================

    /**
     * 推送告警通知
     */
    @Async
    public void pushAlert(AlertDTO alert) {
        try {
            WebSocketMessage<AlertDTO> message = WebSocketMessage.<AlertDTO>builder()
                    .type(WebSocketMessage.MessageType.ALERT)
                    .topic("alert")
                    .data(alert)
                    .build();

            String json = objectMapper.writeValueAsString(message);
            sessionManager.broadcastToType("browser", json);
            
            log.info("Alert pushed: level={}, message={}", alert.getLevel(), alert.getMessage());
        } catch (Exception e) {
            log.error("Failed to push alert: {}", e.getMessage(), e);
        }
    }

    // ==================== 统计数据相关 ====================

    /**
     * 推送统计数据更新
     */
    @Async
    public void pushStatsUpdate(Map<String, Object> stats) {
        try {
            WebSocketMessage<Map<String, Object>> message = WebSocketMessage.<Map<String, Object>>builder()
                    .type(WebSocketMessage.MessageType.STATS_UPDATE)
                    .topic("stats")
                    .data(stats)
                    .build();

            String json = objectMapper.writeValueAsString(message);
            sessionManager.broadcastToTopic("stats", json);
            
            log.trace("Stats update pushed");
        } catch (Exception e) {
            log.error("Failed to push stats update: {}", e.getMessage(), e);
        }
    }

    // ==================== 通用方法 ====================

    /**
     * 广播消息给所有客户端
     */
    @Async
    public void broadcast(String topic, Object data) {
        try {
            WebSocketMessage<Object> message = WebSocketMessage.<Object>builder()
                    .type(WebSocketMessage.MessageType.BROADCAST)
                    .topic(topic)
                    .data(data)
                    .build();

            String json = objectMapper.writeValueAsString(message);
            sessionManager.broadcast(json);
        } catch (Exception e) {
            log.error("Failed to broadcast message: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送消息给指定客户端
     */
    public void sendToClient(String clientId, WebSocketMessage<?> message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            sessionManager.sendToClient(clientId, json);
        } catch (Exception e) {
            log.error("Failed to send message to client {}: {}", clientId, e.getMessage(), e);
        }
    }

    // ==================== 内部 DTO 类 ====================

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ExecutorStatusDTO {
        private String executorId;
        private String address;
        private String status;
        private int runningJobs;
        private double cpuUsage;
        private double memoryUsage;
        private long timestamp;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class JobLogDTO {
        private Long jobId;
        private Long instanceId;
        private String content;
        private String level;
        private long timestamp;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AlertDTO {
        private String id;
        private String level;
        private String type;
        private String message;
        private String source;
        private long timestamp;
    }
}
