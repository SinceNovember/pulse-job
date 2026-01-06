package com.simple.pulsejob.admin.persistence.mapper;

import com.simple.pulsejob.admin.common.model.entity.JobLog;
import com.simple.pulsejob.admin.common.model.enums.LogLevelEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务日志持久化接口.
 */
@Repository
public interface JobLogMapper extends JpaRepository<JobLog, Long>, JpaSpecificationExecutor<JobLog> {

    /**
     * 根据调用ID查询日志
     *
     * @param instanceId 调用ID
     * @return 日志列表（按序号排序）
     */
    List<JobLog> findByInstanceIdOrderBySequenceAsc(Long instanceId);

    /**
     * 分页查询指定调用ID的日志
     *
     * @param instanceId 调用ID
     * @param pageable 分页参数
     * @return 日志分页结果
     */
    Page<JobLog> findByInstanceId(Long instanceId, Pageable pageable);

    /**
     * 根据任务ID查询日志
     *
     * @param jobId 任务ID
     * @return 日志列表
     */
    List<JobLog> findByJobIdOrderByCreateTimeDesc(Integer jobId);

    /**
     * 分页查询指定任务ID的日志
     *
     * @param jobId    任务ID
     * @param pageable 分页参数
     * @return 日志分页结果
     */
    Page<JobLog> findByJobId(Integer jobId, Pageable pageable);

    /**
     * 查询指定执行器的日志
     *
     * @param executorName 执行器名称
     * @param pageable     分页参数
     * @return 日志分页结果
     */
    Page<JobLog> findByExecutorName(String executorName, Pageable pageable);

    /**
     * 查询指定级别的日志
     *
     * @param logLevel 日志级别
     * @param pageable 分页参数
     * @return 日志分页结果
     */
    Page<JobLog> findByLogLevel(LogLevelEnum logLevel, Pageable pageable);

    /**
     * 查询指定时间范围内的日志
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageable  分页参数
     * @return 日志分页结果
     */
    Page<JobLog> findByCreateTimeBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 查询指定调用ID的错误日志
     *
     * @param instanceId 调用ID
     * @return 错误日志列表
     */
    @Query("SELECT l FROM JobLog l WHERE l.instanceId = :instanceId AND l.logLevel = 'ERROR' ORDER BY l.sequence ASC")
    List<JobLog> findErrorLogsByInstanceId(@Param("instanceId") Long instanceId);

    /**
     * 统计指定调用ID的日志数量
     *
     * @param instanceId 调用ID
     * @return 日志数量
     */
    long countByInstanceId(Long instanceId);

    /**
     * 统计指定任务ID的日志数量
     *
     * @param jobId 任务ID
     * @return 日志数量
     */
    long countByJobId(Integer jobId);

    /**
     * 获取指定调用ID的最大序号
     *
     * @param instanceId 调用ID
     * @return 最大序号
     */
    @Query("SELECT COALESCE(MAX(l.sequence), 0) FROM JobLog l WHERE l.instanceId = :instanceId")
    Integer findMaxSequenceByInstanceId(@Param("instanceId") Long instanceId);

    /**
     * 删除指定时间之前的日志（用于日志清理）
     *
     * @param beforeTime 时间点
     * @return 删除的记录数
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM JobLog l WHERE l.createTime < :beforeTime")
    int deleteByCreateTimeBefore(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 删除指定调用ID的所有日志
     *
     * @param instanceId 调用ID
     * @return 删除的记录数
     */
    @Transactional
    @Modifying
    int deleteByInstanceId(Long instanceId);

    /**
     * 删除指定任务ID的所有日志
     *
     * @param jobId 任务ID
     * @return 删除的记录数
     */
    @Transactional
    @Modifying
    int deleteByJobId(Integer jobId);

    /**
     * 批量查询多个调用ID的日志
     *
     * @param instanceIds 调用ID列表
     * @return 日志列表
     */
    @Query("SELECT l FROM JobLog l WHERE l.instanceId IN :instanceIds ORDER BY l.instanceId, l.sequence ASC")
    List<JobLog> findByInstanceIdIn(@Param("instanceIds") List<Long> instanceIds);

    /**
     * 复杂条件查询
     *
     * @param instanceId     调用ID（可选）
     * @param jobId        任务ID（可选）
     * @param executorName 执行器名称（可选）
     * @param logLevel     日志级别（可选）
     * @param startTime    开始时间
     * @param endTime      结束时间
     * @param pageable     分页参数
     * @return 日志分页结果
     */
    @Query("SELECT l FROM JobLog l WHERE " +
           "(:instanceId IS NULL OR l.instanceId = :instanceId) AND " +
           "(:jobId IS NULL OR l.jobId = :jobId) AND " +
           "(:executorName IS NULL OR l.executorName = :executorName) AND " +
           "(:logLevel IS NULL OR l.logLevel = :logLevel) AND " +
           "l.createTime BETWEEN :startTime AND :endTime " +
           "ORDER BY l.createTime DESC")
    Page<JobLog> findByConditions(
            @Param("instanceId") Long instanceId,
            @Param("jobId") Integer jobId,
            @Param("executorName") String executorName,
            @Param("logLevel") LogLevelEnum logLevel,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);
}

