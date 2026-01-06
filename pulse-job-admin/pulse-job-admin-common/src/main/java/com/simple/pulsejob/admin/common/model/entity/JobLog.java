package com.simple.pulsejob.admin.common.model.entity;

import com.simple.pulsejob.admin.common.model.enums.LogLevelEnum;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务执行日志实体.
 * 
 * <p>用于持久化存储任务执行过程中产生的日志</p>
 */
@Data
@Entity
@Table(name = "job_log", indexes = {
    @Index(name = "idx_instance_id", columnList = "instance_id"),
    @Index(name = "idx_job_id", columnList = "job_id"),
    @Index(name = "idx_create_time", columnList = "create_time")
})
public class JobLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 任务实例ID (= job_instance.instanceId) */
    @Column(name = "instance_id", nullable = false)
    private Long instanceId;

    /** 业务任务ID */
    @Column(name = "job_id")
    private Integer jobId;

    /** 执行器名称 */
    @Column(name = "executor_name", length = 128)
    private String executorName;

    /** 执行器地址 */
    @Column(name = "executor_address", length = 256)
    private String executorAddress;

    /** 日志级别 */
    @Column(name = "log_level", nullable = false)
    @Enumerated(EnumType.STRING)
    private LogLevelEnum logLevel;

    /** 日志内容 */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /** 日志序号（同一instanceId下的顺序） */
    @Column(name = "sequence")
    private Integer sequence;

    /** 线程名称 */
    @Column(name = "thread_name", length = 128)
    private String threadName;

    /** Logger名称 */
    @Column(name = "logger_name", length = 256)
    private String loggerName;

    /** 日志原始时间（客户端产生时间） */
    @Column(name = "log_time")
    private LocalDateTime logTime;

    /** 记录创建时间（服务端接收时间） */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    /** 批次号（用于标识同一批次的日志） */
    @Column(name = "batch_id")
    private Long batchId;

    @PrePersist
    public void prePersist() {
        if (this.createTime == null) {
            this.createTime = LocalDateTime.now();
        }
    }

    /**
     * 静态工厂方法
     */
    public static JobLog of(Long instanceId, LogLevelEnum level, String content) {
        JobLog log = new JobLog();
        log.setInstanceId(instanceId);
        log.setLogLevel(level);
        log.setContent(content);
        log.setCreateTime(LocalDateTime.now());
        return log;
    }

    /**
     * 完整构建方法
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final JobLog log = new JobLog();

        public Builder instanceId(Long instanceId) {
            log.setInstanceId(instanceId);
            return this;
        }

        public Builder jobId(Integer jobId) {
            log.setJobId(jobId);
            return this;
        }

        public Builder executorName(String executorName) {
            log.setExecutorName(executorName);
            return this;
        }

        public Builder executorAddress(String executorAddress) {
            log.setExecutorAddress(executorAddress);
            return this;
        }

        public Builder logLevel(LogLevelEnum level) {
            log.setLogLevel(level);
            return this;
        }

        public Builder content(String content) {
            log.setContent(content);
            return this;
        }

        public Builder sequence(Integer sequence) {
            log.setSequence(sequence);
            return this;
        }

        public Builder threadName(String threadName) {
            log.setThreadName(threadName);
            return this;
        }

        public Builder loggerName(String loggerName) {
            log.setLoggerName(loggerName);
            return this;
        }

        public Builder logTime(LocalDateTime logTime) {
            log.setLogTime(logTime);
            return this;
        }

        public Builder batchId(Long batchId) {
            log.setBatchId(batchId);
            return this;
        }

        public JobLog build() {
            if (log.getCreateTime() == null) {
                log.setCreateTime(LocalDateTime.now());
            }
            return log;
        }
    }
}

