package com.simple.pulsejob.admin.business.service;

import com.simple.pulsejob.admin.common.model.base.PageResult;
import com.simple.pulsejob.admin.common.model.entity.JobExecutor;
import com.simple.pulsejob.admin.common.model.enums.RegisterTypeEnum;
import com.simple.pulsejob.admin.common.model.param.JobExecutorParam;
import com.simple.pulsejob.admin.common.model.param.JobExecutorQuery;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.metadata.ExecutorKey;

import java.util.List;
import java.util.Optional;

public interface IJobExecutorService {
    
    /**
     * 分页查询执行器
     * @param query 查询参数
     * @return 分页结果
     */
    PageResult<JobExecutor> pageJobExecutors(JobExecutorQuery query);
    
    /**
     * 添加执行器信息
     *
     * @param jobExecutorParam 执行器信息
     */
    void addJobExecutor(JobExecutorParam jobExecutorParam);

    /**
     * 注册由client通过netty发来的执行器
     * @param channel
     * @param executorWrapper
     */
    void autoRegisterJobExecutor(JChannel channel, ExecutorKey executorWrapper);

    /**
     * 根据ID查询执行器信息
     * @param id 执行器ID
     * @return 执行器信息
     */
    Optional<JobExecutor> getJobExecutorById(Integer id);
    
    /**
     * 根据执行器名称查询执行器信息
     * @param executorName 执行器名称
     * @return 执行器信息
     */
    Optional<JobExecutor> getJobExecutorByName(String executorName);
    
    /**
     * 查询所有执行器信息
     * @return 执行器信息列表
     */
    List<JobExecutor> getAllJobExecutors();
    
    /**
     * 根据注册类型查询执行器信息
     * @param registerType 注册类型
     * @return 执行器信息列表
     */
    List<JobExecutor> getJobExecutorsByRegisterType(RegisterTypeEnum registerType);
    
    /**
     * 更新执行器信息
     * @param jobExecutor 执行器信息
     * @return 更新后的执行器信息
     */
    JobExecutor updateJobExecutor(JobExecutor jobExecutor);
    
    /**
     * 删除执行器信息
     * @param id 执行器ID
     */
    void deleteJobExecutor(Integer id);

    /**
     * 批量添加执行器信息
     * @param jobExecutorParams 执行器信息列表
     */
    void batchAddJobExecutors(List<JobExecutorParam> jobExecutorParams);

    /**
     * 注销对应ip：port的注册执行器
     *
     * @param channel
     */
    void deregisterJobExecutor(String executorName, JChannel channel);

    /**
     * admin启动的时候先清除掉库里的所有executor地址，防止admin断电等强制关闭导致执行器连接信息未清理清理完
     */
    void clearAllJobExecutorAddress();
}