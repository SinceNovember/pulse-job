package com.simple.pulsejob.admin;

import com.simple.pulsejob.admin.business.service.IJobExecutorService;
import com.simple.pulsejob.admin.scheduler.CronJobScheduler;
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

    private final CronJobScheduler cronJobScheduler;

    public void start() throws InterruptedException {
        // 启动前先清空admin所有得执行器地址，防止上次因为强关导致执行器未清空
        jobExecutorService.clearAllJobExecutorAddress();
        if (acceptor.processor() == null) {
            acceptor.withProcessor(acceptorProcessor);
        }
        
        // 启动 Netty 服务器
        acceptor.start();
        
        // 启动定时任务调度器
        cronJobScheduler.start();
        log.info("定时任务调度器已启动");
    }
}
