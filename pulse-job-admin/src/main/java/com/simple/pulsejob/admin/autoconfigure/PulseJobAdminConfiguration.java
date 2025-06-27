package com.simple.pulsejob.admin.autoconfigure;

import java.util.HashMap;
import java.util.Map;
import com.simple.plusejob.serialization.Serializer;
import com.simple.plusejob.serialization.SerializerType;
import com.simple.pulsejob.admin.DefaultServer;
import com.simple.pulsejob.admin.processor.DefaultAcceptorProcessor;
import com.simple.pulsejob.serialization.hessian.HessianSerializer;
import com.simple.pulsejob.serialization.java.JavaSerializer;
import com.simple.pulsejob.transport.JAcceptor;
import com.simple.pulsejob.transport.netty.JNettyTcpAcceptor;
import com.simple.pulsejob.transport.netty.NettyTcpAcceptor;
import com.simple.pulsejob.transport.processor.AcceptorProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(PulseJobAdminProperties.class)
public class PulseJobAdminConfiguration {

    private final PulseJobAdminProperties properties;

    @Bean
    public JAcceptor defaultAcceptor() {
        return new JNettyTcpAcceptor(properties.getPort());
    }

    @Bean
    public Map<Byte, Serializer> serializerMap() {
        Map<Byte, Serializer> serializerMap = new HashMap<>();
        serializerMap.put(SerializerType.JAVA.value(), new JavaSerializer());
        serializerMap.put(SerializerType.HESSIAN.value(), new HessianSerializer());
        return serializerMap;
    }


}
