package com.simple.pulsejob.client.log;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.simple.plusejob.serialization.Serializer;
import com.simple.plusejob.serialization.SerializerType;
import com.simple.plusejob.serialization.io.OutputBuf;
import com.simple.pulsejob.transport.CodecConfig;
import com.simple.pulsejob.transport.JProtocolHeader;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.channel.JFutureListener;
import com.simple.pulsejob.transport.metadata.LogMessage;
import com.simple.pulsejob.transport.payload.JRequestPayload;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

@Slf4j
public class JobLogSenderDisruptor implements InitializingBean, DisposableBean {

    private static final int RING_BUFFER_SIZE = 16384; // 2^14
    private static final int BATCH_MAX_SIZE = 50;
    private static final int BATCH_MAX_BYTES = 16 * 1024;
    private static final long FLUSH_INTERVAL_MS = 300;

    private final Map<Byte, Serializer> serializerMap;
    private final AtomicInteger sequence = new AtomicInteger(0);

    private volatile JChannel channel;

    private Disruptor<LogEvent> disruptor;
    private RingBuffer<LogEvent> ringBuffer;

    public JobLogSenderDisruptor(Map<Byte, Serializer> serializerMap) {
        this.serializerMap = serializerMap;
    }

    public void bindChannel(JChannel channel) {
        this.channel = channel;
    }

    public void sendAsync(LogMessage logMessage) {
        if (logMessage == null || logMessage.getInvokeId() == null) {
            return;
        }
        if (logMessage.getTimestamp() == null) {
            logMessage.setTimestamp(LocalDateTime.now());
        }
        if (logMessage.getSequence() == null) {
            logMessage.setSequence(sequence.incrementAndGet());
        }
        try {
            ringBuffer.publishEvent((event, seq) -> event.set(logMessage));
        } catch (Exception e) {
            log.warn("日志发布失败 invokeId={}", logMessage.getInvokeId(), e);
        }
    }

    @Override
    public void afterPropertiesSet() {
        ThreadFactory tf = r -> {
            Thread t = new Thread(r, "pulse-log-disruptor");
            t.setDaemon(true);
            return t;
        };
        disruptor = new Disruptor<>(LogEvent::new, RING_BUFFER_SIZE, tf,
            ProducerType.MULTI, new BlockingWaitStrategy());
        disruptor.handleEventsWith(this::onEvent);
        disruptor.setDefaultExceptionHandler(new IgnoreExceptionHandler());
        ringBuffer = disruptor.start();
    }

    @Override
    public void destroy() {
        if (disruptor != null) {
            disruptor.shutdown();
        }
    }

    // 聚合批次
    private List<LogMessage> batch = new ArrayList<>(BATCH_MAX_SIZE);
    private Long currentInvokeId = null;
    private int batchBytes = 0;
    private long lastActivity = System.currentTimeMillis();

    private void onEvent(LogEvent event, long sequence, boolean endOfBatch) {
        LogMessage msg = event.msg;
        event.clear();

        if (msg == null) {
            return;
        }

        // invokeId 切换则先刷旧批次
        if (currentInvokeId != null && !currentInvokeId.equals(msg.getInvokeId())) {
            flushBatch();
        }
        currentInvokeId = currentInvokeId == null ? msg.getInvokeId() : currentInvokeId;

        batch.add(msg);
        batchBytes += estimateSize(msg);
        lastActivity = System.currentTimeMillis();

        boolean reachSize = batch.size() >= BATCH_MAX_SIZE || batchBytes >= BATCH_MAX_BYTES;
        boolean timeout = (lastActivity - lastActivity) >= FLUSH_INTERVAL_MS; // 将在 endOfBatch 下方处理
        boolean lastLog = msg.isLast();

        if (reachSize || lastLog || (endOfBatch && timeout)) {
            flushBatch();
        }
    }

    private int estimateSize(LogMessage msg) {
        int size = 32;
        if (msg.getContent() != null) {
            size += msg.getContent().getBytes(StandardCharsets.UTF_8).length;
        }
        return size;
    }

    private void flushBatch() {
        if (currentInvokeId == null || batch.isEmpty()) {
            resetBatch();
            return;
        }

        JChannel current = channel;
        if (current == null || !current.isActive()) {
            resetBatch();
            return;
        }

        Serializer serializer = serializerMap.get(SerializerType.JAVA.value());
        if (serializer == null) {
            log.warn("未找到日志序列化器 code={}", SerializerType.JAVA.value());
            resetBatch();
            return;
        }

        try {
            JRequestPayload payload = new JRequestPayload(currentInvokeId);
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
                    // no-op
                }
                @Override
                public void operationFailure(JChannel channel, Throwable cause) {
                    log.warn("日志批次发送失败 invokeId={}, size={}", currentInvokeId, batch.size(), cause);
                }
            });
        } catch (Throwable t) {
            log.warn("日志批次发送异常 invokeId={}, size={}", currentInvokeId, batch.size(), t);
        } finally {
            resetBatch();
        }
    }

    private void resetBatch() {
        batch = new ArrayList<>(BATCH_MAX_SIZE);
        batchBytes = 0;
        currentInvokeId = null;
        lastActivity = System.currentTimeMillis();
    }

    private static final class LogEvent {
        private LogMessage msg;
        void set(LogMessage msg) { this.msg = msg; }
        void clear() { this.msg = null; }
    }
}