package com.simple.pulsejob.transport.metadata;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 任务执行日志消息 (供执行器与管理端共享).
 */
@Data
public class LogMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 任务调用ID, 复用 invokeId 作为日志归属键 */
    private Long invokeId;

    /** 业务任务ID (可选) */
    private Integer jobId;

    /** 日志级别 */
    private LogLevel level;

    /** 日志内容 */
    private String content;

    /** 日志时间 */
    private LocalDateTime timestamp;

    /** 序号, 便于前端排序 */
    private Integer sequence;

    /** 是否最后一条日志 */
    private boolean last;

    /** 进度百分比 */
    private Integer progress;

    public enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }

    public boolean isError() {
        return level == LogLevel.ERROR;
    }

    public boolean isCompleted() {
        return last;
    }
}

