package com.simple.pulsejob.admin.scheduler;

/**
 * 任务调度器接口
 * <p>
 * 提供给 business 层调用的调度操作接口
 * </p>
 *
 * @author pulse
 */
public interface JobScheduler {

    /**
     * 启动调度器
     */
    void start();

    /**
     * 停止调度器
     */
    void stop();

    /**
     * 调度任务
     *
     * @param context 调度上下文
     */
    void schedule(ScheduleContext context);

    /**
     * 取消任务调度
     *
     * @param jobId 任务ID
     * @return 是否取消成功
     */
    boolean cancel(Long jobId);

    /**
     * 暂停任务
     *
     * @param jobId 任务ID
     * @return 是否暂停成功
     */
    boolean pause(Long jobId);

    /**
     * 恢复任务
     *
     * @param jobId 任务ID
     * @return 是否恢复成功
     */
    boolean resume(Long jobId);

    /**
     * 获取已调度的任务数量
     *
     * @return 任务数量
     */
    int getScheduledCount();

    /**
     * 检查任务是否已调度
     *
     * @param jobId 任务ID
     * @return 是否已调度
     */
    boolean isScheduled(Long jobId);
}

