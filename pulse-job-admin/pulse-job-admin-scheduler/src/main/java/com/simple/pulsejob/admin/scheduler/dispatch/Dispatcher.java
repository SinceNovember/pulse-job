package com.simple.pulsejob.admin.scheduler.dispatch;

import com.simple.pulsejob.admin.scheduler.future.InvokeFuture;
import com.simple.pulsejob.transport.JRequest;

public interface Dispatcher {

    InvokeFuture dispatch(JRequest request);

    DispatchType type();
}
