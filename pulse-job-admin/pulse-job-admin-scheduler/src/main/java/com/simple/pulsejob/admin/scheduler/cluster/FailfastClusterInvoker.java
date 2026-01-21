package com.simple.pulsejob.admin.scheduler.cluster;

import com.simple.pulsejob.admin.common.model.enums.InvokeStrategyEnum;
import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import com.simple.pulsejob.admin.scheduler.dispatch.Dispatcher;
import com.simple.pulsejob.admin.scheduler.factory.DispatcherFactory;
import com.simple.pulsejob.admin.scheduler.future.InvokeFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 快速失败策略.
 *
 * <p>只发起一次调用，失败立即报错。</p>
 * <p>通常用于非幂等写操作，例如新增记录。</p>
 */
@Component
@RequiredArgsConstructor
public class FailfastClusterInvoker implements ClusterInvoker {

    private final DispatcherFactory dispatcherFactory;

    @Override
    public InvokeStrategyEnum strategy() {
        return InvokeStrategyEnum.FAIL_FAST;
    }

    @Override
    public InvokeFuture invoke(ScheduleContext context) throws Exception {
        Dispatcher dispatcher = dispatcherFactory.get(context.getDispatchType());
        return dispatcher.dispatch(context);
    }
}
