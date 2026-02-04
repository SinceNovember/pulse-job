package com.simple.pulsejob.admin.business.service;


import com.simple.pulsejob.admin.common.model.base.PageResult;
import com.simple.pulsejob.admin.common.model.dto.JobInfoWithExecutorDTO;
import com.simple.pulsejob.admin.common.model.entity.JobInfo;
import com.simple.pulsejob.admin.common.model.param.JobInfoParam;
import com.simple.pulsejob.admin.common.model.param.JobInfoQuery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IJobInfoService {
    
    /**
     * 保存任务信息
     *
     * @param jobInfoParam 任务信息
     */
    void addJobInfo(JobInfoParam jobInfoParam);
    
    /**
     * 根据ID查询任务信息
     * @param id 任务ID
     * @return 任务信息
     */
    Optional<JobInfo> getJobInfoById(Integer id);
    
    /**
     * 查询所有任务信息
     * @return 任务信息列表
     */
    List<JobInfo> getAllJobInfos();
    
    /**
     * 根据jobHandler查询任务信息
     * @param jobHandler 任务处理器名称
     * @return 任务信息列表
     */
    List<JobInfo> getJobInfosByHandler(String jobHandler);
    
    /**
     * 根据executorId查询任务信息
     * @param executorId 执行器ID
     * @return 任务信息列表
     */
    List<JobInfo> getJobInfosByExecutorId(Integer executorId);

    /**
     * 更新任务信息
     * @param jobInfo 任务信息
     * @return 更新后的任务信息
     */
    JobInfo updateJobInfo(JobInfo jobInfo);

    /**
     * 删除任务信息
     * @param id 任务ID
     */
    void deleteJobInfo(Integer id);

    /**
     * 批量保存任务信息
     * @param jobInfoParams 任务信息列表
     */
    void batchAddJobInfos(List<JobInfoParam> jobInfoParams);

    /**
     * 查询即将执行的任务
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 即将执行的任务列表
     */
    List<JobInfo> findJobsToExecute(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查询启用的任务
     * @return 启用的任务列表
     */
    List<JobInfo> findEnabledJobs();

    /**
     * 更新任务的下次执行时间
     * @param jobId 任务ID
     * @param nextExecuteTime 下次执行时间
     */
    void updateNextExecuteTime(Integer jobId, LocalDateTime nextExecuteTime);

    /**
     * 更新任务的执行状态
     * @param jobId 任务ID
     * @param lastExecuteTime 上次执行时间
     * @param retryTimes 重试次数
     */
    void updateExecutionStatus(Integer jobId, LocalDateTime lastExecuteTime, Integer retryTimes);

    /**
     * 根据 jobId 获取 JobInfo 及其执行器名称
     */
    Optional<JobInfoWithExecutorDTO> getJobInfoWithExecutorNameById(Integer jobId);

    /**
     * 分页查询任务信息
     * @param query 查询参数
     * @return 分页结果
     */
    PageResult<JobInfo> pageJobInfos(JobInfoQuery query);

    /**
     * 启用/禁用任务
     * @param id 任务ID
     * @param enabled 是否启用
     */
    void setJobEnabled(Integer id, boolean enabled);
} 