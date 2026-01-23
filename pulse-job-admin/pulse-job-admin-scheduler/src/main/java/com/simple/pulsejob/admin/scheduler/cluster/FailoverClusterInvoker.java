package com.simple.pulsejob.admin.scheduler.cluster;

import com.simple.pulsejob.admin.common.model.enums.DispatchTypeEnum;
import com.simple.pulsejob.admin.common.model.enums.InvokeStrategyEnum;
import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import com.simple.pulsejob.admin.scheduler.dispatch.Dispatcher;
import com.simple.pulsejob.admin.scheduler.factory.DispatcherFactory;
import com.simple.pulsejob.admin.scheduler.future.DefaultInvokeFuture;
import com.simple.pulsejob.admin.scheduler.future.FailoverInvokeFuture;
import com.simple.pulsejob.admin.scheduler.future.InvokeFuture;
import com.simple.pulsejob.common.util.StackTraceUtil;
import com.simple.pulsejob.transport.channel.JChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 失败自动切换策略.
 *
 * <p>当出现失败时，重试其它服务器。要注意的是重试会带来更长的延时。</p>
 * <p>建议只用于幂等性操作，通常比较合适用于读操作。</p>
 * <p>注意：failover 不能支持广播的调用方式。</p>
 *
 * <p>重要：重试时复用同一个 instanceId，不会创建新的 JobInstance。</p>
 *
 * @see <a href="https://en.wikipedia.org/wiki/Failover">Failover</a>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FailoverClusterInvoker implements ClusterInvoker {

    private final DispatcherFactory dispatcherFactory;

    @Override
    public InvokeStrategyEnum strategy() {
        return InvokeStrategyEnum.FAIL_OVER;
    }

    @Override
    public InvokeFuture invoke(ScheduleContext context) throws Exception {
        // ⚠️ Failover 不支持广播模式
        if (context.getDispatchType() == DispatchTypeEnum.BROADCAST) {
            throw new UnsupportedOperationException("Failover strategy does not support broadcast dispatch");
        }

        Dispatcher dispatcher = dispatcherFactory.get(context.getDispatchType());
        FailoverInvokeFuture failoverFuture = FailoverInvokeFuture.with();
        int tryCount = context.getRetries() + 1;

        // 第一次调用，会创建 JobInstance
        invoke0(context, dispatcher, tryCount, failoverFuture, null, true);

        return failoverFuture;
    }

    private void invoke0(final ScheduleContext context,
                         final Dispatcher dispatcher,
                         final int tryCount,
                         final FailoverInvokeFuture failoverFuture,
                         final Throwable lastCause,
                         final boolean isFirstAttempt) {
        if (tryCount > 0) {
            final InvokeFuture future;

            if (isFirstAttempt) {
                // 首次调用：正常 dispatch（会触发 beforeTransport 创建 Instance）
                future = dispatcher.dispatch(context);
            } else {
                // 重试调用：跳过 Instance 创建，复用已有的 instanceId
                future = dispatcher.dispatchRetry(context);
            }

            future.whenComplete((response, throwable) -> {
                if (throwable == null) {
                    failoverFuture.complete(response);
                } else {
                    if (log.isWarnEnabled()) {
                        JChannel channel = future instanceof DefaultInvokeFuture
                                ? ((DefaultInvokeFuture) future).channel()
                                : null;

                        log.warn("[{}]: [Fail-over] retry, [{}] attempts left, [handler: {}], {}.",
                                channel != null ? channel.remoteAddress() : "unknown",
                                tryCount - 1,
                                context.getJobHandler(),
                                StackTraceUtil.stackTrace(throwable));
                    }

                    // 递归重试（非首次）
                    invoke0(context, dispatcher, tryCount - 1, failoverFuture, throwable, false);
                }
            });
        } else {
            failoverFuture.completeExceptionally(lastCause != null
                    ? lastCause
                    : new RuntimeException("All retry attempts failed"));
        }
    }
}
