package com.simple.pulsejob.admin.scheduler.dispatch;

import com.simple.pulsejob.admin.common.model.enums.DispatchTypeEnum;
import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import com.simple.pulsejob.admin.scheduler.future.InvokeFuture;

public interface Dispatcher {

    InvokeFuture dispatch(ScheduleContext context);

    DispatchTypeEnum type();
}
