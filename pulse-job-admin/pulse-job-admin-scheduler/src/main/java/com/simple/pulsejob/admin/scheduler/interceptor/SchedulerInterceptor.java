package com.simple.pulsejob.admin.scheduler.interceptor;

import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.JResponse;

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
 * <p>注意：广播/分片模式下，一个 context 可能对应多个 instance</p>
 * <ul>
 *   <li>beforeTransport/afterTransport/onTransportFailure 会被调用多次</li>
 *   <li>通过 {@code request.instanceId()} 区分不同实例</li>
 * </ul>
 *
 * <p>所有方法都提供默认空实现，子类只需覆盖关心的方法。</p>
 */
public interface SchedulerInterceptor {

    /**
     * 调度前置处理（查询 JobInfo、设置 ExecutorKey 等）
     * <p>仅调用一次</p>
     */
    default void beforeSchedule(ScheduleContext context) {}

    /**
     * 网络传输前
     * <p>广播/分片模式下会调用多次，通过 request.instanceId() 区分</p>
     *
     * @param context 调度上下文
     * @param request 当前请求（包含 instanceId）
     */
    default void beforeTransport(ScheduleContext context, JRequest request) {}

    /**
     * 网络传输成功后
     * <p>广播/分片模式下会调用多次</p>
     *
     * @param context 调度上下文
     * @param request 当前请求（包含 instanceId）
     */
    default void afterTransport(ScheduleContext context, JRequest request) {}

    /**
     * 网络传输失败
     * <p>广播/分片模式下会调用多次</p>
     *
     * @param context   调度上下文
     * @param request   当前请求（包含 instanceId）
     * @param throwable 异常信息
     */
    default void onTransportFailure(ScheduleContext context, JRequest request, Throwable throwable) {}

    /**
     * 调度完成（任务执行成功）
     */
    default void afterSchedule(ScheduleContext context, JResponse response) {}

    /**
     * 调度失败（任务执行失败）
     */
    default void onScheduleFailure(ScheduleContext context, JResponse response, Throwable throwable) {}

}