package com.simple.pulsejob.admin.common.model.entity;

import com.simple.pulsejob.admin.common.model.enums.ScheduleTypeEnum;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务信息实体类
 * 用于存储和管理定时任务的基本信息
 * 
 * @author system
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "job_info")
public class JobInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     * 自增长，唯一标识任务
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 任务处理器名称
     * 用于标识具体的任务执行逻辑
     * 例如：dataSyncJob、reportGenerateJob等
     */
    @Column(name = "job_handler", length = 100)
    private String jobHandler;

    /**
     * 调度频率表达式
     * 支持CRON表达式格式
     * 例如：0 0/30 * * * ? (每30分钟执行一次)
     */
    @Column(name = "schedule_rate", length = 100)
    private String scheduleRate;

    /**
     * 调度类型
     * 目前支持CRON表达式调度
     * 后续可扩展支持固定频率、固定延迟等
     */
    @Column(name = "schedule_type")
    private ScheduleTypeEnum scheduleType;

    /**
     * 执行器ID
     * 关联到具体的任务执行器
     * 用于指定任务在哪个执行器上运行
     */
    @Column(name = "executor_id")
    private Integer executorId;

    /**
     * 任务状态
     * 0-禁用：任务暂停执行
     * 1-启用：任务正常执行
     * 默认值为1（启用状态）
     */
    @Column(name = "status")
    private Integer status = 1;

    /**
     * 下次执行时间
     * 系统根据调度规则计算出的下次执行时间
     * 用于任务调度器判断何时执行任务
     */
    @Column(name = "next_execute_time")
    private LocalDateTime nextExecuteTime;

    /**
     * 上次执行时间
     * 记录任务最后一次执行的时间
     * 用于任务执行历史追踪和状态判断
     */
    @Column(name = "last_execute_time")
    private LocalDateTime lastExecuteTime;

    /**
     * 任务描述
     * 对任务的详细说明，便于管理和维护
     * 例如：数据同步任务、报表生成任务等
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 创建时间
     * 记录任务的创建时间
     * 用于任务生命周期管理
     */
    @Column(name = "create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     * 记录任务信息的最后修改时间
     * 用于数据同步和版本控制
     */
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    /**
     * 任务参数
     * 存储任务执行时需要的参数信息
     * JSON格式，支持复杂参数结构
     * 例如：{"dataSource": "mysql", "tableName": "user_info"}
     */
    @Column(name = "job_params", columnDefinition = "TEXT")
    private String jobParams;

    /**
     * 重试次数
     * 记录任务执行失败后的重试次数
     * 用于任务执行状态追踪和重试控制
     * 默认值为0
     */
    @Column(name = "retry_times")
    private Integer retryTimes = 0;

    /**
     * 最大重试次数
     * 任务执行失败时的最大重试次数
     * 超过此次数后任务将停止重试
     * 默认值为3次
     */
    @Column(name = "max_retry_times")
    private Integer maxRetryTimes = 3;

    /**
     * 任务超时时间（秒）
     * 任务执行的最大时间限制
     * 超过此时间任务将被强制终止
     * 默认值为60秒
     */
    @Column(name = "timeout_seconds")
    private Integer timeoutSeconds = 60;

    /**
     * 判断任务是否启用
     * 
     * @return true-启用，false-禁用
     */
    public boolean isEnabled() {
        return status != null && status == 1;
    }

    /**
     * 设置任务启用状态
     * 
     * @param enabled true-启用，false-禁用
     */
    public void setEnabled(boolean enabled) {
        this.status = enabled ? 1 : 0;
    }

    /**
     * 更新任务的下次执行时间
     * 同时更新上次执行时间和更新时间
     * 
     * @param nextExecuteTime 下次执行时间
     */
    public void updateNextExecuteTime(LocalDateTime nextExecuteTime) {
        this.lastExecuteTime = this.nextExecuteTime;
        this.nextExecuteTime = nextExecuteTime;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 重置重试次数
     * 任务执行成功后调用，重置重试计数器
     */
    public void resetRetryTimes() {
        this.retryTimes = 0;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 增加重试次数
     * 任务执行失败时调用
     * 
     * @return 当前重试次数
     */
    public int incrementRetryTimes() {
        this.retryTimes++;
        this.updateTime = LocalDateTime.now();
        return this.retryTimes;
    }

    /**
     * 判断是否达到最大重试次数
     * 
     * @return true-已达到最大重试次数，false-未达到
     */
    public boolean isMaxRetryReached() {
        return this.retryTimes >= this.maxRetryTimes;
    }

    /**
     * 判断任务是否过期
     * 比较下次执行时间与当前时间
     * 
     * @return true-已过期，false-未过期
     */
    public boolean isExpired() {
        return this.nextExecuteTime != null && 
               this.nextExecuteTime.isBefore(LocalDateTime.now());
    }

    /**
     * 获取任务状态描述
     * 
     * @return 状态描述字符串
     */
    public String getStatusDescription() {
        if (status == null) {
            return "未知";
        }
        return status == 1 ? "启用" : "禁用";
    }

} 