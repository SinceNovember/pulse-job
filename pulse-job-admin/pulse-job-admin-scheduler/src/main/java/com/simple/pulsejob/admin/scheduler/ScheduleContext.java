package com.simple.pulsejob.admin.scheduler;

import com.simple.pulsejob.admin.scheduler.dispatch.DispatchType;
import lombok.Data;

@Data
public class ScheduleContext {

    private DispatchType dispatchType;

    private int retries;
}
