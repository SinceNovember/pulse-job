package com.simple.pulsejob.admin.websocket.config;

import com.simple.pulsejob.admin.websocket.handler.PulseJobWebSocketHandler;
import com.simple.pulsejob.admin.websocket.interceptor.WebSocketHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置类
 * 
 * <p>配置 WebSocket 端点和处理器，支持：
 * <ul>
 *   <li>执行器状态实时推送</li>
 *   <li>任务执行日志实时推送</li>
 *   <li>心跳检测</li>
 *   <li>会话管理</li>
 * </ul>
 * 
 * @author PulseJob
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final PulseJobWebSocketHandler webSocketHandler;
    private final WebSocketHandshakeInterceptor handshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册 WebSocket 处理器，允许跨域
        registry.addHandler(webSocketHandler, "/ws/pulse-job")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins("*");
        
        // 支持 SockJS 回退（可选，用于不支持 WebSocket 的浏览器）
        registry.addHandler(webSocketHandler, "/ws/pulse-job/sockjs")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins("*")
                .withSockJS()
                .setHeartbeatTime(25000)
                .setDisconnectDelay(5000);
    }
}
