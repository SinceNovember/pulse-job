package com.simple.pulsejob.admin.scheduler.cluster;

import com.simple.pulsejob.admin.common.model.enums.InvokeStrategyEnum;
import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import com.simple.pulsejob.admin.scheduler.dispatch.Dispatcher;
import com.simple.pulsejob.admin.scheduler.factory.DispatcherFactory;
import com.simple.pulsejob.admin.scheduler.future.FailsafeInvokeFuture;
import com.simple.pulsejob.admin.scheduler.future.InvokeFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 失败安全策略.
 *
 * <p>同步调用时发生异常只打印日志，不抛出异常。</p>
 * <p>通常用于写入审计日志等操作。</p>
 *
 * @see <a href="http://en.wikipedia.org/wiki/Fail-safe">Fail-safe</a>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FailsafeClusterInvoker implements ClusterInvoker {

    private final DispatcherFactory dispatcherFactory;

    @Override
    public InvokeStrategyEnum strategy() {
        return InvokeStrategyEnum.FAIL_SAFE;
    }

    @Override
    public InvokeFuture invoke(ScheduleContext context) throws Exception {
        Dispatcher dispatcher = dispatcherFactory.get(context.getDispatchType());
        InvokeFuture future = dispatcher.dispatch(context);
        return FailsafeInvokeFuture.with(future);
    }
}
