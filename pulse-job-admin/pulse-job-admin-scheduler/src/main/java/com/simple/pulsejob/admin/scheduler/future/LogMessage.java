package com.simple.pulsejob.admin.scheduler.future;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 任务执行日志消息
 */
@Data
public class LogMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 任务调用ID
     */
    private Long invokeId;
    
    /**
     * 任务ID
     */
    private Integer jobId;
    
    /**
     * 日志级别
     */
    private LogLevel level;
    
    /**
     * 日志内容
     */
    private String content;
    
    /**
     * 日志时间
     */
    private LocalDateTime timestamp;
    
    /**
     * 日志序号
     */
    private Integer sequence;
    
    /**
     * 是否是最后一条日志（标记任务结束）
     */
    private boolean last;
    
    /**
     * 执行进度 (0-100)
     */
    private Integer progress;
    
    /**
     * 日志级别枚举
     */
    public enum LogLevel {
        DEBUG, INFO, WARN, ERROR
    }
    
    /**
     * 判断是否是错误日志
     */
    public boolean isError() {
        return level == LogLevel.ERROR;
    }
    
    /**
     * 判断任务是否完成
     */
    public boolean isCompleted() {
        return last;
    }
}

