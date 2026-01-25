package com.simple.pulsejob.admin.common.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 调度实例记录.
 */
@Data
@Entity
@Table(name = "job_instance", indexes = {
    @Index(name = "idx_job_id", columnList = "job_id"),
    @Index(name = "idx_executor_id", columnList = "executor_id")
})
public class JobInstance implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false)
    private Integer jobId;

    @Column(name = "executor_id", nullable = false)
    private Integer executorId;

//    @Column(name = "trigger_time", nullable = false)
    private LocalDateTime triggerTime;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "status", nullable = false)
    private Byte status = 0;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    /** 执行结果（JSON格式） */
    @Column(name = "result", columnDefinition = "TEXT")
    private String result;

    /** 错误信息 */
    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    /** 触发类型：auto-自动调度, manual-手动触发, api-API调用 */
    @Column(name = "trigger_type", length = 20)
    private String triggerType;

    /** 执行器地址（实际执行的地址） */
    @Column(name = "executor_address", length = 100)
    private String executorAddress;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createTime == null) {
            createTime = now;
        }
        if (updateTime == null) {
            updateTime = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = LocalDateTime.now();
    }
}

