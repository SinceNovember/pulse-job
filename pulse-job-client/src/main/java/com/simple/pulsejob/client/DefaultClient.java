package com.simple.pulsejob.client;

import java.util.Map;
import com.simple.plusejob.serialization.Serializer;
import com.simple.plusejob.serialization.io.OutputBuf;
import com.simple.pulsejob.client.autoconfigure.PulseJobClientProperties;
import com.simple.pulsejob.common.util.StringUtil;
import com.simple.pulsejob.transport.CodecConfig;
import com.simple.pulsejob.transport.JConnector;
import com.simple.pulsejob.transport.JProtocolHeader;
import com.simple.pulsejob.transport.UnresolvedSocketAddress;
import com.simple.pulsejob.transport.metadata.ExecutorKey;
import com.simple.pulsejob.transport.netty.JNettyConnection;
import com.simple.pulsejob.transport.netty.JNettyTcpConnector;
import com.simple.pulsejob.transport.netty.channel.NettyChannel;
import com.simple.pulsejob.transport.payload.JRequestPayload;
import com.simple.pulsejob.transport.processor.ConnectorProcessor;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

@Slf4j
public class DefaultClient implements ApplicationListener<ApplicationReadyEvent> {

    private final PulseJobClientProperties properties;
    private final JConnector<JNettyConnection> connector;
    private final ConnectorProcessor clientProcessor;
    private final Map<Byte, Serializer> serializerMap;

    public DefaultClient(PulseJobClientProperties properties, ConnectorProcessor clientProcessor,
                         Map<Byte, Serializer> serializerMap) {
        this.properties = properties;
        this.connector = new JNettyTcpConnector();
        this.clientProcessor = clientProcessor;
        this.serializerMap = serializerMap;
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
        connector.connect(new UnresolvedSocketAddress(
                properties.getAdmin().getHost(), properties.getAdmin().getPort()))
            .operationComplete(operationListener -> log.info("[pulse-job] admin connect success!"));
    }

    private void registerExecutor(Channel channel) {
        ExecutorKey executorWrapper = new ExecutorKey(properties.getExecutorName());

        NettyChannel nettyChannel = NettyChannel.attachChannel(channel);
        Serializer serializer = serializerMap.get((byte) 0x04);

        JRequestPayload requestPayload = new JRequestPayload();
        if (CodecConfig.isCodecLowCopy()) {
            OutputBuf outputBuf =
                serializer.writeObject(nettyChannel.allocOutputBuf(), executorWrapper);
            requestPayload.outputBuf((byte) 0x04, JProtocolHeader.REGISTER_EXECUTOR, outputBuf);
        } else {
            byte[] bytes = serializer.writeObject(executorWrapper);
            requestPayload.bytes((byte) 0x04, JProtocolHeader.REGISTER_EXECUTOR, bytes);
        }
        nettyChannel.write(requestPayload);
    }
}
