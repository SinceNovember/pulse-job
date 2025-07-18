package com.simple.pulsejob.admin.mapper;

import com.simple.pulsejob.admin.model.entity.JobInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobInfoMapper extends JpaRepository<JobInfo, Integer>, JpaSpecificationExecutor<JobInfo> {
    
    /**
     * 根据jobHandler查找任务
     * @param jobHandler 任务处理器名称
     * @return 任务列表
     */
    List<JobInfo> findByJobHandler(String jobHandler);
    
    /**
     * 根据executorId查找任务
     * @param executorId 执行器ID
     * @return 任务列表
     */
    List<JobInfo> findByExecutorId(Integer executorId);

} 