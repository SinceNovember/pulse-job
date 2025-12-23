package com.simple.pulsejob.client.log;

import com.simple.plusejob.serialization.Serializer;
import com.simple.plusejob.serialization.SerializerType;
import com.simple.plusejob.serialization.io.OutputBuf;
import com.simple.pulsejob.common.util.SystemClock;
import com.simple.pulsejob.transport.CodecConfig;
import com.simple.pulsejob.transport.JProtocolHeader;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.channel.JFutureListener;
import com.simple.pulsejob.transport.metadata.LogMessage;
import com.simple.pulsejob.transport.payload.JRequestPayload;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 将客户端日志流式推送给 admin。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobLogSender {

    private final Map<Byte, Serializer> serializerMap;

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
        while (started.get()) {
            try {
                LogMessage msg = queue.poll(5, TimeUnit.SECONDS);
                if (msg != null) {
                    doSend(msg);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable t) {
                log.warn("发送日志时异常", t);
            }
        }
    }

    private void doSend(LogMessage msg) {
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

        JRequestPayload payload = new JRequestPayload(msg.getInvokeId());
        if (CodecConfig.isCodecLowCopy()) {
            OutputBuf outputBuf = serializer.writeObject(current.allocOutputBuf(), msg);
            payload.outputBuf(SerializerType.JAVA.value(), JProtocolHeader.JOB_LOG_MESSAGE, outputBuf);
        } else {
            byte[] bytes = serializer.writeObject(msg);
            payload.bytes(SerializerType.JAVA.value(), JProtocolHeader.JOB_LOG_MESSAGE, bytes);
        }
        current.write(payload, new JFutureListener<>() {
            @Override
            public void operationSuccess(JChannel channel) {
                log.info("Response sent success");
            }

            @Override
            public void operationFailure(JChannel channel, Throwable cause) {
                long duration = SystemClock.millisClock().now();
                log.error("Response sent failed, duration: {} millis, channel: {}, cause: {}.",
                    duration, channel, cause);
            }
        });
    }
}

