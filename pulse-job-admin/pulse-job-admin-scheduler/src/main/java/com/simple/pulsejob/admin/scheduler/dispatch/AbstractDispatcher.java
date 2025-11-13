package com.simple.pulsejob.admin.scheduler.dispatch;

import java.util.List;
import com.simple.pulsejob.admin.scheduler.channel.ExecutorChannelGroupManager;
import com.simple.pulsejob.admin.scheduler.interceptor.ScheduleInterceptor;
import com.simple.pulsejob.admin.scheduler.load.balance.LoadBalancer;
import com.simple.pulsejob.common.util.StackTraceUtil;
import com.simple.pulsejob.common.util.SystemClock;
import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.JResponse;
import com.simple.pulsejob.transport.Status;
import com.simple.pulsejob.transport.channel.CopyOnWriteGroupList;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.channel.JChannelGroup;
import com.simple.pulsejob.transport.channel.JFutureListener;
import com.simple.pulsejob.transport.metadata.ExecutorKey;
import com.simple.pulsejob.transport.metadata.MessageWrapper;
import com.simple.pulsejob.transport.metadata.ResultWrapper;
import com.simple.pulsejob.transport.payload.JRequestPayload;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.net.DispatchType;

@Slf4j
@SuperBuilder
public abstract class AbstractDispatcher implements Dispatcher {

    private final ExecutorChannelGroupManager channelGroupManager;

    private final List<ScheduleInterceptor> interceptors;

    private final LoadBalancer loadBalancer;

    protected JChannel select(ExecutorKey executorKey) {
        CopyOnWriteGroupList groups = channelGroupManager.find(executorKey);
        JChannelGroup group = loadBalancer.select(groups, executorKey);
        if (groups != null) {
            if (group.isAvailable()) {
                return group.next();
            }

            long deadline = group.deadlineMillis();
            if (deadline > 0 && SystemClock.millisClock().now() > deadline) {
                boolean removed = groups.remove(group);
                if (removed) {
                    if (log.isWarnEnabled()) {
                        log.warn("Removed channel group: {} in executor: {} on [select].",
                            group, executorKey.exeuctorKeyString());
                    }
                }
            }
        }
        throw new IllegalStateException("No connections");
    }

    protected void write(
        final JChannel channel, final JRequest request, final DispatchType dispatchType) {
        final MessageWrapper message = request.getMessage();
//        final DefaultInvokeFuture<T> future = DefaultInvokeFuture
//            .with(request.invokeId(), channel, timeoutMillis, returnType, dispatchType)
//            .interceptors(interceptors);

        if (interceptors != null) {
            for (int i = 0; i < interceptors.size(); i++) {
                interceptors.get(i).beforeSchedule(request, channel);
            }
        }

        final JRequestPayload payload = request.payload();

        channel.write(payload, new JFutureListener<JChannel>() {

            @Override
            public void operationSuccess(JChannel channel) throws Exception {
                // 标记已发送
//                future.markSent();

                if (dispatchType == DispatchType.ROUND) {
                    payload.clear();
                }
            }

            @Override
            public void operationFailure(JChannel channel, Throwable cause) throws Exception {
                if (dispatchType == DispatchType.ROUND) {
                    payload.clear();
                }

                if (log.isWarnEnabled()) {
                    log.warn("Writes {} fail on {}, {}.", request, channel, StackTraceUtil.stackTrace(cause));
                }

                ResultWrapper result = new ResultWrapper();
                result.setError(new JupiterRemoteException(cause));

                JResponse response = new JResponse(payload.invokeId());
                response.status(Status.CLIENT_ERROR);
                response.result(result);

                DefaultInvokeFuture.fakeReceived(channel, response, dispatchType);
            }
        });

        return future;
    }
}
