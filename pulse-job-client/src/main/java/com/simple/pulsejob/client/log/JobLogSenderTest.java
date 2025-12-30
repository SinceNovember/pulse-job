package com.simple.pulsejob.client.log;

import com.simple.plusejob.serialization.Serializer;
import com.simple.plusejob.serialization.SerializerType;
import com.simple.plusejob.serialization.io.OutputBuf;
import com.simple.pulsejob.transport.CodecConfig;
import com.simple.pulsejob.transport.JProtocolHeader;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.channel.JFutureListener;
import com.simple.pulsejob.transport.metadata.LogMessage;
import com.simple.pulsejob.transport.payload.JRequestPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 将客户端日志流式推送给 admin。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobLogSenderTest {

    private final Map<Byte, Serializer> serializerMap;

    private static final int BATCH_MAX_SIZE = 50;
    private static final int BATCH_MAX_BYTES = 16 * 1024;
    private static final long FLUSH_INTERVAL_MS = 300;

    private final BlockingQueue<LogMessage> queue = new LinkedBlockingQueue<>(4096);
    private final ExecutorService worker =
        Executors.newSingleThreadExecutor(r -> new Thread(r, "pulse-log-sender"));
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicInteger sequence = new AtomicInteger(0);

    private volatile JChannel channel;

    public void bindChannel(JChannel channel) {
        this.channel = channel;
        startIfNeeded();
    }

    public void sendAsync(LogMessage logMessage) {
        if (logMessage == null || logMessage.getInvokeId() == null) {
            return;
        }
        // 填充缺省字段
        if (logMessage.getTimestamp() == null) {
            logMessage.setTimestamp(LocalDateTime.now());
        }
        if (logMessage.getSequence() == null) {
            logMessage.setSequence(sequence.incrementAndGet());
        }
        if (!queue.offer(logMessage)) {
            log.warn("日志队列已满，丢弃日志 invokeId={}", logMessage.getInvokeId());
        }
    }

    private void startIfNeeded() {
        if (started.compareAndSet(false, true)) {
            worker.submit(this::drainLoop);
        }
    }

    private void drainLoop() {
        List<LogMessage> batch = new ArrayList<>(BATCH_MAX_SIZE);
        Long currentInvokeId = null;
        int batchBytes = 0;
        long lastActivity = System.currentTimeMillis();

        while (started.get()) {
            try {
                LogMessage msg = queue.poll(FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS);
                long now = System.currentTimeMillis();
                if (msg != null) {
                    // 若遇到不同 invokeId，先刷掉已有批次
                    if (currentInvokeId != null && !currentInvokeId.equals(msg.getInvokeId())) {
                        flushBatch(currentInvokeId, batch);
                        batch = new ArrayList<>(BATCH_MAX_SIZE);
                        batchBytes = 0;
                    }
                    currentInvokeId = currentInvokeId == null ? msg.getInvokeId() : currentInvokeId;
                    batch.add(msg);
                    batchBytes += estimateSize(msg);
                    lastActivity = now;
                }

                boolean timeout = !batch.isEmpty() && (now - lastActivity) >= FLUSH_INTERVAL_MS;
                boolean reachSize = batch.size() >= BATCH_MAX_SIZE || batchBytes >= BATCH_MAX_BYTES;
                boolean lastLog = msg != null && msg.isLast();

                if (!batch.isEmpty() && (reachSize || timeout || lastLog)) {
                    flushBatch(currentInvokeId, batch);
                    batch = new ArrayList<>(BATCH_MAX_SIZE);
                    batchBytes = 0;
                    currentInvokeId = null;
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable t) {
                log.warn("发送日志时异常", t);
            }
        }

        if (!batch.isEmpty() && currentInvokeId != null) {
            flushBatch(currentInvokeId, batch);
        }
    }

    private int estimateSize(LogMessage msg) {
        int size = 32; // base overhead
        if (msg.getContent() != null) {
            size += msg.getContent().getBytes(StandardCharsets.UTF_8).length;
        }
        return size;
    }

    private void flushBatch(Long invokeId, List<LogMessage> batch) {
        if (invokeId == null || batch.isEmpty()) {
            return;
        }

        JChannel current = channel;
        if (current == null || !current.isActive()) {
            // 无可用连接，直接忽略（可按需落盘）
            return;
        }

        Serializer serializer = serializerMap.get(SerializerType.JAVA.value());
        if (serializer == null) {
            log.warn("未找到日志序列化器 code=0x04");
            return;
        }

        JRequestPayload payload = new JRequestPayload(invokeId);
        if (CodecConfig.isCodecLowCopy()) {
            OutputBuf outputBuf = serializer.writeObject(current.allocOutputBuf(), batch);
            payload.outputBuf(SerializerType.JAVA.value(), JProtocolHeader.JOB_LOG_MESSAGE, outputBuf);
        } else {
            byte[] bytes = serializer.writeObject(batch);
            payload.bytes(SerializerType.JAVA.value(), JProtocolHeader.JOB_LOG_MESSAGE, bytes);
        }
        current.write(payload, new JFutureListener<>() {
            @Override
            public void operationSuccess(JChannel channel) {
                // 日志发送成功无需频繁打印，避免刷屏
            }

            @Override
            public void operationFailure(JChannel channel, Throwable cause) {
                log.warn("日志批次发送失败 invokeId={}, size={}", invokeId, batch.size(), cause);
            }
        });
    }
}

