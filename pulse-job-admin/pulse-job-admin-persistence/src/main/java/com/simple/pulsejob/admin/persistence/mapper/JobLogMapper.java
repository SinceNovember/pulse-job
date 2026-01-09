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
     * 分页查询指定调用ID的日志
     *
     * @param instanceId 调用ID
     * @param pageable 分页参数
     * @return 日志分页结果
     */
    Page<JobLog> findByInstanceId(Long instanceId, Pageable pageable);

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
     * 统计指定调用ID的日志数量
     *
     * @param instanceId 调用ID
     * @return 日志数量
     */
    long countByInstanceId(Long instanceId);

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

}

