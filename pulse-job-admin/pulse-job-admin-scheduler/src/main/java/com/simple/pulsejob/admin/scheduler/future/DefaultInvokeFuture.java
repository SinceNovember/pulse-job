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
import java.util.concurrent.TimeUnit;

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
    private volatile boolean sent = false;

    private List<JobInterceptor> interceptors;

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

    public static void received(JChannel channel, JResponse response) {
        long invokeId = response.id();

        DefaultInvokeFuture future = roundFutures.remove(invokeId);

        if (future == null) {
            future = broadcastFutures.remove(subInvokeId(channel.id(), invokeId));
        }

        future.doReceived(response);

    }

    private void doReceived(JResponse response) {
        byte status = response.status();
        if (status == Status.OK.value()) {
            ResultWrapper wrapper = response.result();
            complete(wrapper.getResult());
        }

        List<JobInterceptor> interceptors = this.interceptors;
        if (interceptors != null) {
            for (int i = interceptors.size() - 1; i >= 0; i--) {
                interceptors.get(i).afterInvoke(response, channel);
            }
        }
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
}
