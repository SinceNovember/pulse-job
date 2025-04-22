package com.simple.pulsejob.admin;

import com.simple.pulsejob.admin.processor.DefaultAcceptorProcessor;
import com.simple.pulsejob.transport.JAcceptor;
import com.simple.pulsejob.transport.processor.AcceptorProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultServer {

    private final JAcceptor acceptor;

    private final AcceptorProcessor acceptorProcessor;

    public void start() throws InterruptedException {
        if (acceptor.processor() == null) {
            acceptor.withProcessor(acceptorProcessor);
        }
        acceptor.start();
    }
}
