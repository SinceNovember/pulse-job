package com.simple.pulsejob.admin.scheduler;

import com.simple.pulsejob.admin.common.model.entity.JobInfo;

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
     * 调度任务到时间轮
     *
     * @param jobInfo 任务信息
     */
    void schedule(JobInfo jobInfo);

    /**
     * 手动触发任务（立即执行）
     *
     * @param executorName 执行器名称
     * @param jobId        任务ID
     * @param executorId   执行器ID
     * @param handlerName  处理器名称
     * @param params       任务参数
     */
    void trigger(String executorName, Long jobId, Long executorId, String handlerName, String params);

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

