package com.simple.pulsejob.client.log;

import com.simple.plusejob.serialization.SerializerType;
import com.simple.pulsejob.transport.JProtocolHeader;
import com.simple.pulsejob.transport.JResponse;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.channel.JFutureListener;
import com.simple.pulsejob.transport.metadata.BatchLogMessage;
import com.simple.pulsejob.transport.metadata.LogMessage;
import com.simple.pulsejob.transport.payload.JRequestPayload;
import com.simple.pulsejob.transport.payload.JResponsePayload;
import com.simple.pulsejob.transport.payload.PayloadSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 批量日志发送器.
 *
 * <p>设计简洁：单线程处理，poll阻塞等待 + drainTo批量拉取</p>
 */
@Slf4j
@Component
public class JobLogSender implements SmartLifecycle {

    /**
     * 批量大小阈值
     */
    @Value("${pulse-job.log.batch.size:200}")
    private int batchSize;

    /**
     * 最大等待时间（毫秒），超过则发送当前批次
     */
    @Value("${pulse-job.log.batch.max-wait-ms:1000}")
    private long maxWaitMs;

    /**
     * 队列最大容量
     */
    @Value("${pulse-job.log.batch.queue-capacity:8192}")
    private int queueCapacity;

    /**
     * 日志缓冲队列
     */
    private BlockingQueue<LogMessage> queue;

    private final Object lock = new Object();

    /**
     * 工作线程
     */
    private ExecutorService worker;
    private ScheduledExecutorService scheduler;
    private ExecutorService sender; // 专门负责发送

    private final List<LogMessage> buffer = new ArrayList<>();
    /**
     * 状态控制
     */
    private final AtomicBoolean started = new AtomicBoolean(false);
    private volatile boolean running = false;

    /**
     * 全局序号生成器
     */
    private final AtomicInteger globalSequence = new AtomicInteger(0);

    /**
     * 当前活跃的 Channel
     */
    private volatile JChannel channel;

    @PostConstruct
    public void init() {
        if (batchSize <= 0) batchSize = 200;
        if (maxWaitMs <= 0) maxWaitMs = 1000;
        if (queueCapacity <= 0) queueCapacity = 8192;

        queue = new LinkedBlockingQueue<>(queueCapacity);

        this.worker = Executors.newSingleThreadExecutor(
                r -> new Thread(r, "log-consumer"));

        this.scheduler = Executors.newSingleThreadScheduledExecutor(
                r -> new Thread(r, "log-flush-timer"));

        this.sender = Executors.newSingleThreadExecutor(
                r -> new Thread(r, "log-sender"));

        log.info("JobLogSender 初始化, batchSize={}, maxWaitMs={}, queueCapacity={}",
                batchSize, maxWaitMs, queueCapacity);
    }

    @Override
    public void start() {
        if (started.compareAndSet(false, true)) {
            this.running = true;
            doStart(); // 原来的 start() 内容
        }
    }

    @Override
    public void stop() {
        this.running = false;
        shutdown(); // flush + 关线程池
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE; // 最后启动，最先关闭
    }

    private void doStart() {
        // 1. 时间兜底 flush
        scheduler.scheduleAtFixedRate(
                this::flushSafely,
                maxWaitMs,
                maxWaitMs,
                TimeUnit.MILLISECONDS);

        // 2. 消费线程
        worker.submit(this::consumeLoop);
    }


    private void consumeLoop() {
        try {
            while (running) {
                LogMessage msg = queue.take(); // 永远阻塞
                List<LogMessage> toSend = null;

                synchronized (lock) {
                    buffer.add(msg);
                    if (buffer.size() >= batchSize) {
                        toSend = drainBuffer();
                    }
                }

                if (toSend != null) {
                    submitSend(toSend);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            // 退出前兜底 flush
            flushSafely();
        }
    }

    private void flushSafely() {
        List<LogMessage> toSend;
        synchronized (lock) {
            toSend = drainBuffer();
        }
        if (toSend != null) {
            submitSend(toSend);
        }
    }

    private List<LogMessage> drainBuffer() {
        if (buffer.isEmpty()) {
            return null;
        }
        List<LogMessage> batch = new ArrayList<>(buffer);
        buffer.clear();
        return batch;
    }

    private void submitSend(List<LogMessage> batch) {
        sender.submit(() -> {
            try {
                doSendBatch(batch);
            } catch (Throwable t) {
                // 这里可以扩展：重试 / 落盘 / fallback
                log.warn("发送日志批次失败, size={}", batch.size(), t);
            }
        });
    }

    public void shutdown() {
        running = false;

        // 1. 停止生产 & 消费
        worker.shutdownNow();
        scheduler.shutdownNow();

        // 2. 主线程兜底 flush
        flushSafely();

        // 3. 等待 sender 把已提交任务发完
        sender.shutdown();
    }

    public void bindChannel(JChannel channel) {
        this.channel = channel;
    }

    public void sendAsync(LogMessage logMessage) {
        if (logMessage == null || logMessage.getInstanceId() == null) {
            return;
        }

        fillDefaults(logMessage);

        if (!queue.offer(logMessage)) {
            log.warn("日志队列已满，丢弃日志 instanceId={}", logMessage.getInstanceId());
        }
    }


    private void doSendBatch(List<LogMessage> logs) {
        if (logs.isEmpty()) {
            return;
        }

        JChannel current = channel;
        if (current == null || !current.isActive()) {
            log.debug("无可用连接，丢弃 {} 条日志", logs.size());
            return;
        }

        try {
            JResponsePayload payload = PayloadSerializer.response()
                    .channel(current)
                    .type(SerializerType.JAVA)
                    .message(BatchLogMessage.of(logs))
                    .messageCode(JProtocolHeader.JOB_BATCH_LOG_MESSAGE)
                    .build();

            int count = logs.size();
            current.write(payload, new JFutureListener<>() {
                @Override
                public void operationSuccess(JChannel channel) {
                    log.debug("日志发送成功, count={}", count);
                }

                @Override
                public void operationFailure(JChannel channel, Throwable cause) {
                    log.warn("日志发送失败, count={}", count);
                }
            });
        } catch (Exception e) {
            log.error("发送日志异常", e);
        }
    }

    private void fillDefaults(LogMessage logMessage) {
        if (logMessage.getTimestamp() == null) {
            logMessage.setTimestamp(LocalDateTime.now());
        }
        if (logMessage.getSequence() == null) {
            logMessage.setSequence(globalSequence.incrementAndGet());
        }
    }
}
