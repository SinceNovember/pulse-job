package com.simple.pulsejob.admin.business.service;

/**
 * 任务触发服务接口.
 *
 * <p>专门处理任务触发逻辑，与 JobInfoService 分离以避免循环依赖</p>
 */
public interface IJobTriggerService {

    /**
     * 手动触发任务
     *
     * @param jobId 任务ID
     */
    void trigger(Integer jobId);

    /**
     * 手动触发任务（带参数）
     *
     * @param jobId  任务ID
     * @param params 任务参数（覆盖默认参数）
     */
    void trigger(Integer jobId, String params);
}
