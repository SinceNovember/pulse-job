package com.simple.pulsejob.admin.scheduler.future;

import com.simple.pulsejob.admin.scheduler.dispatch.DispatchType;
import com.simple.pulsejob.admin.scheduler.interceptor.JobInterceptor;
import com.simple.pulsejob.common.util.Maps;
import com.simple.pulsejob.transport.JResponse;
import com.simple.pulsejob.transport.Status;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.metadata.ResultWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 默认异步调用 Future
 * 支持流式日志接收
 */
public class DefaultInvokeFuture extends CompletableFuture<Object> implements InvokeFuture {

    private static final ConcurrentMap<Long, DefaultInvokeFuture> roundFutures =
            Maps.newConcurrentMapLong(1024);
    private static final ConcurrentMap<String, DefaultInvokeFuture> broadcastFutures =
            Maps.newConcurrentMap(1024);
    private static final Logger log = LoggerFactory.getLogger(DefaultInvokeFuture.class);

    private final long invokeId;
    private final JChannel channel;
    private final Class<?> returnType;
    private final long timeout;
    private final DispatchType dispatchType;
    private volatile boolean sent = false;

    private List<JobInterceptor> interceptors;
    
    // ✅ 日志监听器列表（支持多个监听器）
    private final CopyOnWriteArrayList<Consumer<LogMessage>> logListeners = new CopyOnWriteArrayList<>();
    
    // ✅ 日志历史缓存（用于新订阅者获取历史日志）
    private final CopyOnWriteArrayList<LogMessage> logHistory = new CopyOnWriteArrayList<>();

    public static DefaultInvokeFuture with(
            long invokeId, JChannel channel, long timeoutMillis, Class<?> returnType, DispatchType dispatchType) {
        return new DefaultInvokeFuture(invokeId, channel, timeoutMillis, returnType, dispatchType);
    }

    public DefaultInvokeFuture(
            long invokeId, JChannel channel, long timeoutMillis, Class<?> returnType, DispatchType dispatchType) {
        this.invokeId = invokeId;
        this.channel = channel;
        this.timeout = timeoutMillis > 0 ? TimeUnit.MILLISECONDS.toNanos(timeoutMillis) : 500;
        this.returnType = returnType;
        this.dispatchType = dispatchType;

        switch (dispatchType) {
            case ROUND:
                roundFutures.put(invokeId, this);
                break;
            case BROADCAST:
                String channelId = channel.id();
                broadcastFutures.put(subInvokeId(channelId, invokeId), this);
                break;
            default:
                throw new IllegalArgumentException("Unsupported " + dispatchType);
        }
    }

    public JChannel channel() {
        return channel;
    }

    private static String subInvokeId(String channelId, long invokeId) {
        return channelId + invokeId;
    }

    /**
     * ✅ 接收最终响应（任务执行结果）
     */
    public static void received(JChannel channel, JResponse response) {
        long invokeId = response.id();

        DefaultInvokeFuture future = roundFutures.get(invokeId);

        if (future == null) {
            future = broadcastFutures.get(subInvokeId(channel.id(), invokeId));
        }

        if (future != null) {
            future.doReceived(response);
        } else {
            log.warn("未找到对应的 Future: invokeId={}", invokeId);
        }
    }
    
    /**
     * ✅ 接收流式日志消息（不会完成 Future）
     */
    public static void receivedLog(JChannel channel, long invokeId, LogMessage logMessage) {
        DefaultInvokeFuture future = roundFutures.get(invokeId);
        
        if (future == null) {
            future = broadcastFutures.get(subInvokeId(channel.id(), invokeId));
        }
        
        if (future != null) {
            future.receiveLog(logMessage);
        } else {
            log.warn("未找到对应的 Future: invokeId={}", invokeId);
        }
    }

    /**
     * 处理最终响应
     */
    private void doReceived(JResponse response) {
        byte status = response.status();
        
        if (status == Status.OK.value()) {
            ResultWrapper wrapper = response.result();
            complete(wrapper.getResult());
        } else {
            // 任务执行失败
            ResultWrapper wrapper = response.result();
            completeExceptionally(wrapper.getError());
        }

        // 拦截器后置处理
        List<JobInterceptor> interceptors = this.interceptors;
        if (interceptors != null) {
            for (int i = interceptors.size() - 1; i >= 0; i--) {
                interceptors.get(i).afterInvoke(response, channel);
            }
        }
        
        // ✅ 清理 Future
        cleanup();
    }
    
    /**
     * ✅ 接收日志消息
     */
    @Override
    public void receiveLog(LogMessage logMessage) {
        log.debug("接收日志: invokeId={}, level={}, content={}", 
            invokeId, logMessage.getLevel(), logMessage.getContent());
        
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
            log.info("收到最后一条日志: invokeId={}", invokeId);
        }
    }
    
    /**
     * ✅ 添加日志监听器
     */
    @Override
    public InvokeFuture addLogListener(Consumer<LogMessage> logListener) {
        logListeners.add(logListener);
        
        // 推送历史日志给新订阅者
        for (LogMessage log : logHistory) {
            try {
                logListener.accept(log);
            } catch (Exception e) {
                log.error("推送历史日志失败", e);
            }
        }
        
        return this;
    }
    
    /**
     * ✅ 标记任务完成
     */
    @Override
    public void markCompleted(Object result) {
        complete(result);
        cleanup();
    }
    
    /**
     * ✅ 标记任务失败
     */
    @Override
    public void markFailed(Throwable cause) {
        completeExceptionally(cause);
        cleanup();
    }
    
    /**
     * 清理资源
     */
    private void cleanup() {
        // 从缓存中移除
        switch (dispatchType) {
            case ROUND:
                roundFutures.remove(invokeId);
                break;
            case BROADCAST:
                broadcastFutures.remove(subInvokeId(channel.id(), invokeId));
                break;
        }
        
        // 清理日志缓存（可选，保留一段时间供查询）
        // logHistory.clear();
        // logListeners.clear();
    }

    @Override
    public Object getResult() throws Throwable {
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

    public DefaultInvokeFuture interceptors(List<JobInterceptor> interceptors) {
        this.interceptors = interceptors;
        return this;
    }
    
    /**
     * 获取日志历史
     */
    public List<LogMessage> getLogHistory() {
        return List.copyOf(logHistory);
    }
}
