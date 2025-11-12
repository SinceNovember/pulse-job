package com.simple.pulsejob.admin.scheduler.dispatch;

import com.simple.pulsejob.transport.JRequest;

public interface Dispatcher {

    void dispatch(JRequest request);
}
