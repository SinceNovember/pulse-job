package com.simple.pulsejob.admin.scheduler.interceptor;

import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.JResponse;
import com.simple.pulsejob.transport.channel.JChannel;

/**
 * 调度拦截器接口.
 *
 * <p>调度生命周期：</p>
 * <pre>
 * beforeSchedule → beforeTransport → [网络传输] → afterTransport → [等待执行结果] → afterSchedule
 *                         ↓                              ↓
 *                  onTransportFailure            onScheduleFailure
 * </pre>
 *
 * <p>所有方法都提供默认空实现，子类只需覆盖关心的方法。</p>
 */
public interface SchedulerInterceptor {

    /**
     * 调度前置处理（查询 JobInfo、设置 ExecutorKey 等）
     */
    default void beforeSchedule(ScheduleContext context) {}

    /**
     * 网络传输前（创建 JobInstance）
     */
    default void beforeTransport(ScheduleContext context, JChannel channel) {}

    /**
     * 网络传输后（标记已发送、注册回调）
     */
    default void afterTransport(ScheduleContext context, JChannel channel, JRequest request) {}

    /**
     * 网络传输失败
     */
    default void onTransportFailure(ScheduleContext context, JChannel channel, JRequest request, Throwable throwable) {}

    /**
     * 调度完成（任务执行成功）
     */
    default void afterSchedule(ScheduleContext context, JChannel channel, JResponse response) {}

    /**
     * 调度失败（任务执行失败）
     */
    default void onScheduleFailure(ScheduleContext context, JChannel channel, JRequest request, Throwable throwable) {}

}