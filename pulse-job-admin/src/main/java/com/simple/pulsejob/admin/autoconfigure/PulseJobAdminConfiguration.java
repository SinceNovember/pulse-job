package com.simple.pulsejob.admin.autoconfigure;

import com.simple.pulsejob.admin.DefaultServer;
import com.simple.pulsejob.admin.processor.DefaultAcceptorProcessor;
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
    public AcceptorProcessor defaultAcceptorProcessor() {
        return new DefaultAcceptorProcessor();
    }

}
