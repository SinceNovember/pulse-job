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
    @Index(name = "idx_create_time", columnList = "create_time")
})
public class JobLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 任务实例ID (= job_instance.instanceId) */
    @Column(name = "job_id", nullable = false)
    private Integer jobId;

    /** 任务实例ID (= job_instance.instanceId) */
    @Column(name = "instance_id", nullable = false)
    private Long instanceId;

    /** 日志级别 */
    @Column(name = "log_level", nullable = false)
    @Enumerated(EnumType.STRING)
    private LogLevelEnum logLevel;

    /** 日志内容 */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /** 记录创建时间（服务端接收时间） */
    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;


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

        public Builder logLevel(LogLevelEnum level) {
            log.setLogLevel(level);
            return this;
        }



        public Builder content(String content) {
            log.setContent(content);
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

