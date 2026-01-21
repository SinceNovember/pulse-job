package com.simple.pulsejob.admin.scheduler.dispatch;

import com.simple.pulsejob.admin.common.model.enums.DispatchTypeEnum;
import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import com.simple.pulsejob.admin.scheduler.future.InvokeFuture;

public interface Dispatcher {

    /**
     * 分发任务（首次调用，会创建 JobInstance）
     */
    InvokeFuture dispatch(ScheduleContext context);

    /**
     * 重试分发（复用已有的 instanceId，不创建新的 JobInstance）
     *
     * <p>用于 Failover 策略重试场景</p>
     */
    InvokeFuture dispatchRetry(ScheduleContext context);

    DispatchTypeEnum type();
}
