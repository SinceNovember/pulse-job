package com.simple.pulsejob.admin;

import com.simple.pulsejob.admin.business.service.IJobExecutorService;
import com.simple.pulsejob.admin.scheduler.JScheduler;
import com.simple.pulsejob.transport.JAcceptor;
import com.simple.pulsejob.transport.processor.AcceptorProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminNettyServer {

    private final JAcceptor acceptor;

    private final AcceptorProcessor acceptorProcessor;

    private final IJobExecutorService jobExecutorService;

    private final JScheduler jobScheduler;

    public void start() throws InterruptedException {
        // 启动前先清空admin所有得执行器地址，防止上次因为强关导致执行器未清空
        jobExecutorService.clearAllJobExecutorAddress();
        if (acceptor.processor() == null) {
            acceptor.withProcessor(acceptorProcessor);
        }
        acceptor.start();

    }
}
