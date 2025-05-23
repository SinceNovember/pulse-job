package com.simple.pulsejob.admin.service;

import com.simple.pulsejob.admin.model.entity.JobInfo;

import java.util.List;
import java.util.Optional;

public interface IJobInfoService {
    
    /**
     * 保存任务信息
     * @param jobInfo 任务信息
     * @return 保存后的任务信息
     */
    JobInfo saveJobInfo(JobInfo jobInfo);
    
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
     * 根据cronType查询任务信息
     * @param cronType cron类型
     * @return 任务信息列表
     */
    List<JobInfo> getJobInfosByCronType(Short cronType);
    
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
     * @param jobInfos 任务信息列表
     * @return 保存后的任务信息列表
     */
    List<JobInfo> batchSaveJobInfos(List<JobInfo> jobInfos);
} 