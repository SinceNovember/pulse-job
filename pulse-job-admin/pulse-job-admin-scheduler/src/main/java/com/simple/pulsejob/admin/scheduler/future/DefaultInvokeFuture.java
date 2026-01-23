package com.simple.pulsejob.admin.scheduler.future;

import com.simple.pulsejob.admin.common.model.enums.DispatchTypeEnum;
import com.simple.pulsejob.admin.scheduler.interceptor.SchedulerInterceptor;
import com.simple.pulsejob.common.util.Maps;
import com.simple.pulsejob.transport.JResponse;
import com.simple.pulsejob.transport.Status;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.metadata.LogMessage;
import com.simple.pulsejob.transport.metadata.ResultWrapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * 默认异步调用 Future
 * 支持流式日志接收
 */
@Slf4j
public class DefaultInvokeFuture extends CompletableFuture<JResponse> implements InvokeFuture {

    private static final ConcurrentMap<Long, DefaultInvokeFuture> roundFutures =
            Maps.newConcurrentMapLong(1024);
    private static final ConcurrentMap<String, DefaultInvokeFuture> broadcastFutures =
            Maps.newConcurrentMap(1024);

    /**
     * ✅ 根据 instanceId 获取 Future（用于外部注册回调）
     */
    public static DefaultInvokeFuture getFuture(long instanceId) {
        return roundFutures.get(instanceId);
    }

    /**
     * ✅ 根据 channel 和 instanceId 获取广播 Future
     */
    public static DefaultInvokeFuture getBroadcastFuture(String channelId, long instanceId) {
        return broadcastFutures.get(subInstanceId(channelId, instanceId));
    }

    /** 超时调度器（守护线程，共享） */
    private static final ScheduledExecutorService TIMEOUT_SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "invoke-future-timeout");
                t.setDaemon(true);
                return t;
            });

    private final long instanceId;
    private final JChannel channel;
    private final Class<?> returnType;
    private final long timeout;
    private final DispatchTypeEnum dispatchType;
    private volatile boolean sent = false;

    private List<SchedulerInterceptor> interceptors;

    /** 超时清理任务 */
    private volatile ScheduledFuture<?> timeoutTask;
    
    // ✅ 日志监听器列表（支持多个监听器）
    private final CopyOnWriteArrayList<Consumer<LogMessage>> logListeners = new CopyOnWriteArrayList<>();
    
    // ✅ 日志历史缓存（用于新订阅者获取历史日志）
    private final CopyOnWriteArrayList<LogMessage> logHistory = new CopyOnWriteArrayList<>();

    public static DefaultInvokeFuture with(
            long instanceId, JChannel channel, long timeoutMillis, Class<?> returnType, DispatchTypeEnum dispatchType) {
        return new DefaultInvokeFuture(instanceId, channel, timeoutMillis, returnType, dispatchType);
    }

    public DefaultInvokeFuture(
            long instanceId, JChannel channel, long timeoutMillis, Class<?> returnType, DispatchTypeEnum dispatchType) {
        this.instanceId = instanceId;
        this.channel = channel;
        this.timeout = timeoutMillis > 0 ? timeoutMillis : 30000; // 默认 30 秒
        this.returnType = returnType;
        this.dispatchType = dispatchType;

        switch (dispatchType) {
            case ROUND:
                roundFutures.put(instanceId, this);
                break;
            case BROADCAST:
                String channelId = channel.id();
                broadcastFutures.put(subInstanceId(channelId, instanceId), this);
                break;
            default:
                throw new IllegalArgumentException("Unsupported " + dispatchType);
        }

        // ✅ 注册超时任务
//        scheduleTimeout();
    }

    /**
     * 注册超时清理任务
     */
    private void scheduleTimeout() {
        this.timeoutTask = TIMEOUT_SCHEDULER.schedule(() -> {
            if (!isDone()) {
                log.warn("Future 超时: instanceId={}, timeout={}ms", instanceId, timeout);
                completeExceptionally(new TimeoutException("Invoke timeout after " + timeout + "ms"));
                cleanup();
            }
        }, timeout, TimeUnit.MILLISECONDS);
    }

    public JChannel channel() {
        return channel;
    }

    private static String subInstanceId(String channelId, long instanceId) {
        return channelId + instanceId;
    }

    /**
     * ✅ 接收最终响应（任务执行结果）
     */
    public static void received(JChannel channel, JResponse response) {
        long instanceId = response.instanceId();

        DefaultInvokeFuture future = roundFutures.get(instanceId);

        if (future == null) {
            future = broadcastFutures.get(subInstanceId(channel.id(), instanceId));
        }

        if (future != null) {
            future.doReceived(response);
        } else {
            log.warn("未找到对应的 Future: instanceId={}", instanceId);
        }
    }
    
    /**
     * ✅ 接收流式日志消息（不会完成 Future）
     */
    public static void receivedLog(JChannel channel, long instanceId, LogMessage logMessage) {
        DefaultInvokeFuture future = roundFutures.get(instanceId);
        
        if (future == null) {
            future = broadcastFutures.get(subInstanceId(channel.id(), instanceId));
        }
        
        if (future != null) {
            future.receiveLog(logMessage);
        } else {
            log.warn("未找到对应的 Future: instanceId={}", instanceId);
        }
    }

    /**
     * 处理最终响应
     */
    private void doReceived(JResponse response) {
//        byte status = response.status();
//        ResultWrapper wrapper = response.result();
//
//        if (status == Status.OK.value()) {
//            log.info("任务id:{}, 执行完成", response.instanceId());
//            complete(response);
//        } else {
//            // 任务执行失败，向上游回传异常
//            Object result = wrapper != null ? wrapper.getResult() : null;
//            Throwable cause = (result instanceof Throwable)
//                    ? (Throwable) result
//                    : new RuntimeException("任务执行失败, status=" + Status.parse(status) + ", result=" + result);
//            completeExceptionally(cause);
//        }
        complete(response);
        // ✅ 清理 Future
        cleanup();
    }
    
    /**
     * ✅ 接收日志消息
     */
    public void receiveLog(LogMessage logMessage) {
        log.debug("接收日志: instanceId={}, level={}, content={}", 
            instanceId, logMessage.getLevel(), logMessage.getContent());
        
        // 1. 缓存日志
        logHistory.add(logMessage);
        
        // 2. 通知所有监听器
        for (Consumer<LogMessage> listener : logListeners) {
            try {
                listener.accept(logMessage);
            } catch (Exception e) {
                log.error("日志监听器异常", e);
            }
        }
        
        // 3. 如果是最后一条日志，可以标记为即将结束
        if (logMessage.isLast()) {
            log.info("收到最后一条日志: instanceId={}", instanceId);
        }
    }
    
    /**
     * ✅ 添加日志监听器
     */
    public InvokeFuture addLogListener(Consumer<LogMessage> logListener) {
        logListeners.add(logListener);
        
        // 推送历史日志给新订阅者
        for (LogMessage log : logHistory) {
            try {
                logListener.accept(log);
            } catch (Exception e) {
//                log.error("推送历史日志失败", e);
            }
        }
        
        return this;
    }
    

    /**
     * ✅ 标记任务失败
     */
    public void markFailed(Throwable cause) {
        completeExceptionally(cause);
        cleanup();
    }
    
    /**
     * 清理资源
     */
    private void cleanup() {
        // ✅ 取消超时任务
        ScheduledFuture<?> task = this.timeoutTask;
        if (task != null && !task.isDone()) {
            task.cancel(false);
        }

        // 从缓存中移除
        switch (dispatchType) {
            case ROUND:
                roundFutures.remove(instanceId);
                break;
            case BROADCAST:
                broadcastFutures.remove(subInstanceId(channel.id(), instanceId));
                break;
        }
        
        // 清理日志监听器
        logListeners.clear();
    }

    @Override
    public JResponse getResult() throws Throwable {
        try {
            return get();
        } catch (Exception e) {
            log.error("Channel:[{}] getResult error", channel.remoteAddress());
            throw new RuntimeException(e);
        }
    }

    public void markSent() {
        sent = true;
    }


    /**
     * 获取日志历史
     */
    public List<LogMessage> getLogHistory() {
        return List.copyOf(logHistory);
    }
}
