package com.simple.pulsejob.client;

import com.simple.pulsejob.client.autoconfigure.PulseJobClientProperties;
import com.simple.pulsejob.common.util.StringUtil;
import com.simple.pulsejob.transport.JConnector;
import com.simple.pulsejob.transport.UnresolvedSocketAddress;
import com.simple.pulsejob.transport.netty.JNettyConnection;
import com.simple.pulsejob.transport.netty.JNettyTcpConnector;
import com.simple.pulsejob.transport.processor.ConnectorProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Pulse Job 客户端.
 *
 * <p>实现 {@link SmartLifecycle} 支持：</p>
 * <ul>
 *   <li>应用启动后自动连接 Admin</li>
 *   <li>应用关闭时优雅断开连接</li>
 * </ul>
 */
@Slf4j
public class DefaultClient implements SmartLifecycle {

    private static final int DEFAULT_PHASE = Integer.MAX_VALUE - 100;

    private final PulseJobClientProperties properties;
    private final ConnectorProcessor clientProcessor;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<JNettyConnection> connectionRef = new AtomicReference<>();

    private volatile JConnector<JNettyConnection> connector;

    public DefaultClient(PulseJobClientProperties properties, ConnectorProcessor clientProcessor) {
        this.properties = properties;
        this.clientProcessor = clientProcessor;
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            log.warn("[pulse-job] Client is already running");
            return;
        }

        if (!isAdminConfigured()) {
            log.warn("[pulse-job] Admin host is empty, skip connecting");
            return;
        }

        doConnect();
    }

    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        log.info("[pulse-job] Shutting down client...");

        // 关闭连接
        JNettyConnection connection = connectionRef.getAndSet(null);
        if (connection != null) {
            connection.setReconnect(false);
        }

        // 关闭 Connector
        if (connector != null) {
            connector.shutdownGracefully();
            connector = null;
        }

        log.info("[pulse-job] Client shutdown complete");
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return DEFAULT_PHASE;
    }


    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        JNettyConnection connection = connectionRef.get();
        return connection != null
                && connection.getFuture().channel() != null
                && connection.getFuture().channel().isActive();
    }

    /**
     * 获取当前连接
     */
    public JNettyConnection getConnection() {
        return connectionRef.get();
    }

    /**
     * 手动重连
     */
    public void reconnect() {
        if (!running.get()) {
            log.warn("[pulse-job] Client is not running, cannot reconnect");
            return;
        }

        JNettyConnection oldConnection = connectionRef.get();
        if (oldConnection != null) {
            oldConnection.setReconnect(false);
        }

        doConnect();
    }

    private boolean isAdminConfigured() {
        return properties.getAdmin() != null
                && StringUtil.isNotBlank(properties.getAdmin().getHost());
    }

    private void doConnect() {
        String host = properties.getAdmin().getHost();
        int port = properties.getAdmin().getPort();

        log.info("[pulse-job] Connecting to admin {}:{}...", host, port);

        try {
            if (connector == null) {
                connector = new JNettyTcpConnector();
                connector.withProcessor(clientProcessor);
            }

            JNettyConnection connection = connector.connect(new UnresolvedSocketAddress(host, port), true);
            connection.setReconnect(true);

            connection.operationComplete(isSuccess -> {
                    log.info("[pulse-job] Connected to admin {}:{}", host, port);
                    connectionRef.set(connection);
            });

        } catch (Exception e) {
            log.error("[pulse-job] Failed to connect to admin {}:{}", host, port, e);
        }
    }
}
