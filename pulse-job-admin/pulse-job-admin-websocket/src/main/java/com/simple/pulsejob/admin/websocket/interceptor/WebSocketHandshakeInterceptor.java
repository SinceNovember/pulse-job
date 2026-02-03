package com.simple.pulsejob.admin.websocket.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手拦截器
 * 
 * <p>用于在 WebSocket 握手阶段进行：
 * <ul>
 *   <li>身份验证</li>
 *   <li>权限检查</li>
 *   <li>请求参数提取</li>
 *   <li>会话属性设置</li>
 * </ul>
 * 
 * @author PulseJob
 */
@Slf4j
@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private static final String CLIENT_ID_PARAM = "clientId";
    private static final String CLIENT_TYPE_PARAM = "clientType";
    private static final String TOKEN_PARAM = "token";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        
        String clientId = null;
        String clientType = "browser"; // 默认为浏览器客户端
        String token = null;
        String remoteAddress = null;

        if (request instanceof ServletServerHttpRequest servletRequest) {
            // 从查询参数中提取信息
            clientId = servletRequest.getServletRequest().getParameter(CLIENT_ID_PARAM);
            String typeParam = servletRequest.getServletRequest().getParameter(CLIENT_TYPE_PARAM);
            if (typeParam != null && !typeParam.isEmpty()) {
                clientType = typeParam;
            }
            token = servletRequest.getServletRequest().getParameter(TOKEN_PARAM);
            remoteAddress = servletRequest.getServletRequest().getRemoteAddr();
        }

        // 生成默认客户端ID
        if (clientId == null || clientId.isEmpty()) {
            clientId = generateClientId(remoteAddress);
        }

        // TODO: 根据需要添加 token 验证逻辑
        // if (!validateToken(token)) {
        //     log.warn("WebSocket handshake rejected: invalid token for client {}", clientId);
        //     return false;
        // }

        // 设置会话属性
        attributes.put("clientId", clientId);
        attributes.put("clientType", clientType);
        attributes.put("remoteAddress", remoteAddress);
        attributes.put("connectTime", System.currentTimeMillis());

        log.info("WebSocket handshake started: clientId={}, clientType={}, remoteAddress={}", 
                clientId, clientType, remoteAddress);
        
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("WebSocket handshake failed", exception);
        } else {
            log.debug("WebSocket handshake completed successfully");
        }
    }

    /**
     * 生成客户端ID
     */
    private String generateClientId(String remoteAddress) {
        return "client_" + System.currentTimeMillis() + "_" + 
               (remoteAddress != null ? remoteAddress.hashCode() : Math.random() * 10000);
    }

    /**
     * 验证 Token（预留方法）
     */
    @SuppressWarnings("unused")
    private boolean validateToken(String token) {
        // TODO: 实现 token 验证逻辑
        return true;
    }
}
