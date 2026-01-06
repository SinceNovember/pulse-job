package com.simple.pulsejob.admin.scheduler.instance;

import com.simple.pulsejob.admin.common.model.entity.JobInstance;
import com.simple.pulsejob.admin.common.model.enums.JobInstanceStatus;

import java.time.LocalDateTime;

/**
 * 任务实例管理器接口.
 *
 * <p>定义在 scheduler 模块，由 business 模块实现</p>
 */
public interface JobInstanceManager {

    /**
     * 创建任务实例（调度前调用）
     *
     * @param jobId      任务ID
     * @param executorId 执行器ID
     * @return 创建的实例（包含生成的ID）
     */
    JobInstance createInstance(Long jobId, Long executorId);

    /**
     * 更新实例状态为已发送
     */
    void markDispatched(Long instanceId);

    /**
     * 更新实例状态为执行中
     */
    void markRunning(Long instanceId, LocalDateTime startTime);

    /**
     * 更新实例状态为成功
     */
    void markSuccess(Long instanceId, LocalDateTime endTime);

    /**
     * 更新实例状态为失败
     */
    void markFailed(Long instanceId, LocalDateTime endTime, String errorMsg);

    /**
     * 更新实例状态为超时
     */
    void markTimeout(Long instanceId);

    /**
     * 更新实例状态
     */
    void updateStatus(Long instanceId, JobInstanceStatus status);

    /**
     * 根据ID查询实例
     */
    JobInstance getById(Long instanceId);

    /**
     * 增加重试次数
     */
    void incrementRetryCount(Long instanceId);
}

