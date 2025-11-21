package com.simple.pulsejob.admin.scheduler.cluster;

import com.simple.pulsejob.admin.scheduler.dispatch.Dispatcher;
import com.simple.pulsejob.admin.scheduler.dispatch.RoundRobinDispatcher;
import com.simple.pulsejob.admin.scheduler.future.DefaultInvokeFuture;
import com.simple.pulsejob.admin.scheduler.future.FailoverInvokeFuture;
import com.simple.pulsejob.admin.scheduler.future.InvokeFuture;
import com.simple.pulsejob.common.util.Reflects;
import com.simple.pulsejob.common.util.Requires;
import com.simple.pulsejob.common.util.StackTraceUtil;
import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.metadata.MessageWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 失败自动切换, 当出现失败, 重试其它服务器, 要注意的是重试会带来更长的延时.
 *
 * 建议只用于幂等性操作, 通常比较合适用于读操作.
 *
 * 注意failover不能支持广播的调用方式.
 *
 * https://en.wikipedia.org/wiki/Failover
 *
 * jupiter
 * org.jupiter.rpc.consumer.cluster
 *
 * @author jiachun.fjc
 */
@Slf4j
public class FailoverClusterInvoker implements ClusterInvoker {

    private final Dispatcher dispatcher;

    private final int retries;

    public FailoverClusterInvoker(Dispatcher dispatcher, int retries) {
        Requires.requireTrue(
            dispatcher instanceof RoundRobinDispatcher,
            Reflects.simpleClassName(dispatcher) + " is unsupported [FailoverClusterInvoker]"
        );

        this.dispatcher = dispatcher;
        this.retries = retries;
    }

    @Override
    public Strategy strategy() {
        return Strategy.FAIL_OVER;
    }

    @Override
    public InvokeFuture invoke(JRequest request) throws Exception {
        FailoverInvokeFuture future = FailoverInvokeFuture.with();
        int tryCount = retries + 1;
        invoke0(request, tryCount, future, null);

        return future;
    }

    private void invoke0(final JRequest request,
                         final int tryCount,
                         final FailoverInvokeFuture failOverFuture,
                         final Throwable lastCause) {
        if (tryCount > 0) {
            final InvokeFuture future = dispatcher.dispatch(request);
            future.whenComplete((result, throwable) -> {
                if (throwable == null) {
                    failOverFuture.complete(request);
                } else {
                    if (log.isWarnEnabled()) {
                        MessageWrapper message = request.getMessage();
                        JChannel channel =
                            future instanceof DefaultInvokeFuture ? ((DefaultInvokeFuture) future).channel() : null;

                        log.warn("[{}]: [Fail-over] retry, [{}] attempts left, [method: {}], {}.",
                            channel,
                            tryCount - 1,
                            message.getMethodName(),
                            StackTraceUtil.stackTrace(throwable));
                    }
                    invoke0(request, tryCount - 1, failOverFuture, throwable);
                }
            });
        } else {
            failOverFuture.completeExceptionally(lastCause);
        }
    }
}
