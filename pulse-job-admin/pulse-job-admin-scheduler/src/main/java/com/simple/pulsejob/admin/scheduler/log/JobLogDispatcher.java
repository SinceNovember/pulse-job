package com.simple.pulsejob.admin.scheduler.log;

import com.simple.pulsejob.common.concurrent.JNamedThreadFactory;
import com.simple.pulsejob.transport.metadata.LogMessage;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 任务日志派发器.
 *
 * <p>收集所有 {@link JobLogListener} 实现，统一派发日志事件。</p>
 * <ul>
 *   <li>支持同步/异步监听器</li>
 *   <li>按优先级排序执行</li>
 *   <li>异常隔离，单个监听器失败不影响其他</li>
 * </ul>
 */
@Slf4j
@Component
public class JobLogDispatcher {

    private final List<JobLogListener> listeners;
    private ExecutorService asyncExecutor;

    public JobLogDispatcher(List<JobLogListener> listeners) {
        // 按优先级排序
        this.listeners = listeners.stream()
                .sorted(Comparator.comparingInt(JobLogListener::getOrder))
                .toList();
        log.info("已注册 {} 个日志监听器: {}", listeners.size(),
                listeners.stream().map(l -> l.getClass().getSimpleName()).toList());
    }

    @PostConstruct
    public void init() {
        asyncExecutor = Executors.newFixedThreadPool(2,
                new JNamedThreadFactory("job-log-dispatch", true));
    }

    @PreDestroy
    public void destroy() {
        if (asyncExecutor != null) {
            asyncExecutor.shutdown();
            try {
                if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    asyncExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                asyncExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 派发单条日志到所有监听器
     *
     * @param logMessage 日志消息
     */
    public void dispatch(LogMessage logMessage) {
        if (logMessage == null) {
            return;
        }
        for (JobLogListener listener : listeners) {
            invokeListener(listener, () -> listener.onLog(logMessage));
        }
    }

    /**
     * 派发批量日志到所有监听器
     *
     * @param logs 日志列表
     */
    public void dispatchBatch(List<LogMessage> logs) {
        if (logs == null || logs.isEmpty()) {
            return;
        }
        for (JobLogListener listener : listeners) {
            invokeListener(listener, () -> listener.onBatchLog(logs));
        }
    }

    /**
     * 调用监听器（根据同步/异步配置）
     */
    private void invokeListener(JobLogListener listener, Runnable task) {
        if (listener.isAsync()) {
            asyncExecutor.submit(() -> safeInvoke(listener, task));
        } else {
            safeInvoke(listener, task);
        }
    }

    /**
     * 安全调用，异常隔离
     */
    private void safeInvoke(JobLogListener listener, Runnable task) {
        try {
            task.run();
        } catch (Exception e) {
            log.error("日志监听器 {} 执行异常", listener.getClass().getSimpleName(), e);
        }
    }
}

