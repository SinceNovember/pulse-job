package com.simple.pulsejob.client.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.task.TaskExecutor;

public class PulseJobExecutor {
    public void executeTask(Long taskId, Runnable runnable) {
        try {
            MDC.put("taskId", taskId.toString());  // 绑定任务 ID
            Logger logger = LoggerFactory.getLogger(TaskExecutor.class);
            logger.info("任务 {} 开始执行", taskId);

            // 任务业务逻辑
            runnable.run();

            logger.info("任务 {} 执行完成", taskId);
        } finally {
            MDC.remove("taskId");  // 任务结束后清除 MDC 避免污染其他日志
        }
    }

    private void doWork() {
        // 模拟任务逻辑
        try { Thread.sleep(2000); } catch (InterruptedException e) { }
    }
}
