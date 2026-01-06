package com.simple.pulsejob.transport.metadata;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量日志消息（供执行器批量推送日志到管理端）.
 * 
 * <p>设计目的：减少网络IO，提高日志传输效率</p>
 */
@Data
@AllArgsConstructor
public class BatchLogMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 默认批量大小 */
    public static final int DEFAULT_BATCH_SIZE = 100;
    
    /** 最大批量大小 */
    public static final int MAX_BATCH_SIZE = 500;

    /** 批量日志列表（每条日志保留自己的 instanceId） */
    private List<LogMessage> logs;

    /**
     * 静态工厂方法：从日志列表创建批量消息
     */
    public static BatchLogMessage of(List<LogMessage> logs) {
        return new BatchLogMessage(logs);
    }

    /**
     * 添加单条日志
     */
    public BatchLogMessage addLog(LogMessage log) {
        if (logs == null) {
            logs = new ArrayList<>();
        }
        logs.add(log);
        return this;
    }

    /**
     * 批量添加日志
     */
    public BatchLogMessage addLogs(List<LogMessage> logList) {
        if (logs == null) {
            logs = new ArrayList<>();
        }
        logs.addAll(logList);
        return this;
    }

    /**
     * 获取日志条数
     */
    public int size() {
        return logs == null ? 0 : logs.size();
    }

    /**
     * 是否已满
     */
    public boolean isFull() {
        return size() >= DEFAULT_BATCH_SIZE;
    }

    /**
     * 是否为空
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * 获取日志列表
     */
    public List<LogMessage> getLogs() {
        return logs != null ? logs : new ArrayList<>();
    }
}
