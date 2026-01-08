package com.simple.pulsejob.admin.scheduler.invoker;

import com.simple.pulsejob.admin.scheduler.ScheduleConfig;

/**
 * 任务调用器接口.
 *
 * <p>所有调度都会创建 JobInstance，使用 instanceId 作为请求标识</p>
 */
public interface Invoker {

    Object invoke(ScheduleConfig scheduleConfig) throws Throwable;
}
