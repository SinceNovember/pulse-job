package com.simple.pulsejob.admin.business.service;

import com.simple.pulsejob.admin.common.model.entity.JobLog;
import com.simple.pulsejob.admin.common.model.enums.LogLevelEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务日志服务接口.
 * 
 * <p>提供日志的存储、查询、清理等业务功能</p>
 */
public interface IJobLogService {

    // ==================== 存储相关 ====================

    /**
     * 保存单条日志
     *
     * @param jobLog 日志实体
     * @return 保存后的日志实体
     */
    JobLog save(JobLog jobLog);

    /**
     * 批量保存日志
     *
     * @param jobLogs 日志列表
     * @return 保存后的日志列表
     */
    List<JobLog> saveBatch(List<JobLog> jobLogs);

    // ==================== 查询相关 ====================


    /**
     * 分页查询指定调用ID的日志
     *
     * @param instanceId 调用ID
     * @param pageable 分页参数
     * @return 日志分页结果
     */
    Page<JobLog> findByInstanceId(Long instanceId, Pageable pageable);



    // ==================== 统计相关 ====================

    /**
     * 统计指定调用ID的日志数量
     *
     * @param instanceId 调用ID
     * @return 日志数量
     */
    long countByInstanceId(Long instanceId);


    // ==================== 删除/清理相关 ====================

    /**
     * 删除指定调用ID的日志
     *
     * @param instanceId 调用ID
     * @return 删除的记录数
     */
    int deleteByInstanceId(Long instanceId);

    /**
     * 清理过期日志
     *
     * @param retentionDays 保留天数
     * @return 删除的记录数
     */
    int cleanExpiredLogs(int retentionDays);

    /**
     * 清理指定时间之前的日志
     *
     * @param beforeTime 时间点
     * @return 删除的记录数
     */
    int cleanLogsBefore(LocalDateTime beforeTime);
}

