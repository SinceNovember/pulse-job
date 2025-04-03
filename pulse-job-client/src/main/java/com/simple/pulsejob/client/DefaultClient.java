package com.simple.pulsejob.client;

import com.simple.pulsejob.client.autoconfigure.PulseJobProperties;
import com.simple.pulsejob.common.util.StringUtil;
import com.simple.pulsejob.transport.JConnection;
import com.simple.pulsejob.transport.JConnector;
import com.simple.pulsejob.transport.UnresolvedSocketAddress;
import com.simple.pulsejob.transport.netty.JNettyTcpConnector;
import com.simple.pulsejob.transport.processor.ConnectorProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

@Slf4j
public class DefaultClient implements ApplicationListener<ApplicationReadyEvent> {

    private final PulseJobProperties properties;
    private final JConnector<JConnection> connector;
    private final ConnectorProcessor clientProcessor;

    public DefaultClient(PulseJobProperties properties, ConnectorProcessor clientProcessor) {
        this.properties = properties;
        this.connector = new JNettyTcpConnector();
        this.clientProcessor = clientProcessor;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (StringUtil.isBlank(properties.getAdmin().getHost())) {
            log.info("Admin host is empty, connect failed");
            return;
        }
        if (connector.processor() == null) {
            connector.withProcessor(clientProcessor);

        }
        JConnection connect = connector.connect(new UnresolvedSocketAddress(
            properties.getAdmin().getHost(), properties.getAdmin().getPort()));
        connect.operationComplete(operationListener -> {
            log.info("[pulse-job] admin connect success!");
        });

    }
}
