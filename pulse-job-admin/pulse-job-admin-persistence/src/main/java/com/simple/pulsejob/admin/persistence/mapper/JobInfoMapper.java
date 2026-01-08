package com.simple.pulsejob.admin.persistence.mapper;

import com.simple.pulsejob.admin.common.model.dto.JobInfoWithExecutorDTO;
import com.simple.pulsejob.admin.common.model.entity.JobInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    /**
     * 查询启用的任务
     * @return 启用的任务列表
     */
    List<JobInfo> findByStatus(Integer status);

    /**
     * 查询即将执行的任务
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param status 任务状态
     * @return 即将执行的任务列表
     */
    @Query("SELECT j FROM JobInfo j WHERE j.nextExecuteTime BETWEEN :startTime AND :endTime AND j.status = :status")
    List<JobInfo> findJobsToExecute(@Param("startTime") LocalDateTime startTime, 
                                   @Param("endTime") LocalDateTime endTime, 
                                   @Param("status") Integer status);

    /**
     * 更新任务的下次执行时间
     * @param jobId 任务ID
     * @param nextExecuteTime 下次执行时间
     */
    @Modifying
    @Query("UPDATE JobInfo j SET j.nextExecuteTime = :nextExecuteTime, j.updateTime = :updateTime WHERE j.id = :jobId")
    void updateNextExecuteTime(@Param("jobId") Integer jobId, 
                              @Param("nextExecuteTime") LocalDateTime nextExecuteTime,
                              @Param("updateTime") LocalDateTime updateTime);

    /**
     * 更新任务的执行状态
     * @param jobId 任务ID
     * @param lastExecuteTime 上次执行时间
     * @param retryTimes 重试次数
     * @param updateTime 更新时间
     */
    @Modifying
    @Query("UPDATE JobInfo j SET j.lastExecuteTime = :lastExecuteTime, j.retryTimes = :retryTimes, j.updateTime = :updateTime WHERE j.id = :jobId")
    void updateExecutionStatus(@Param("jobId") Integer jobId, 
                              @Param("lastExecuteTime") LocalDateTime lastExecuteTime,
                              @Param("retryTimes") Integer retryTimes,
                              @Param("updateTime") LocalDateTime updateTime);

    /**
     * 关联查询任务与执行器名称
     */
    @Query("select new com.simple.pulsejob.admin.common.model.dto.JobInfoWithExecutorDTO(j, e.executorName) "
         + "from JobInfo j left join JobExecutor e on j.executorId = e.id "
         + "where j.id = :jobId")
    Optional<JobInfoWithExecutorDTO> findWithExecutorNameById(@Param("jobId") Integer jobId);
} 