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
     * 根据调用ID查询日志
     *
     * @param invokeId 调用ID
     * @return 日志列表（按序号排序）
     */
    List<JobLog> findByInvokeId(Long invokeId);

    /**
     * 分页查询指定调用ID的日志
     *
     * @param invokeId 调用ID
     * @param pageable 分页参数
     * @return 日志分页结果
     */
    Page<JobLog> findByInvokeId(Long invokeId, Pageable pageable);

    /**
     * 根据任务ID查询日志
     *
     * @param jobId 任务ID
     * @return 日志列表
     */
    List<JobLog> findByJobId(Integer jobId);

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
    Page<JobLog> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 查询指定调用ID的错误日志
     *
     * @param invokeId 调用ID
     * @return 错误日志列表
     */
    List<JobLog> findErrorLogs(Long invokeId);

    /**
     * 复杂条件查询
     *
     * @param invokeId     调用ID（可选）
     * @param jobId        任务ID（可选）
     * @param executorName 执行器名称（可选）
     * @param logLevel     日志级别（可选）
     * @param startTime    开始时间
     * @param endTime      结束时间
     * @param pageable     分页参数
     * @return 日志分页结果
     */
    Page<JobLog> search(Long invokeId, Integer jobId, String executorName,
                        LogLevelEnum logLevel, LocalDateTime startTime,
                        LocalDateTime endTime, Pageable pageable);

    // ==================== 统计相关 ====================

    /**
     * 统计指定调用ID的日志数量
     *
     * @param invokeId 调用ID
     * @return 日志数量
     */
    long countByInvokeId(Long invokeId);

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
     * @param invokeId 调用ID
     * @return 最大序号
     */
    int getMaxSequence(Long invokeId);

    // ==================== 删除/清理相关 ====================

    /**
     * 删除指定调用ID的日志
     *
     * @param invokeId 调用ID
     * @return 删除的记录数
     */
    int deleteByInvokeId(Long invokeId);

    /**
     * 删除指定任务ID的日志
     *
     * @param jobId 任务ID
     * @return 删除的记录数
     */
    int deleteByJobId(Integer jobId);

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

