package com.simple.pulsejob.admin.scheduler.log;

import com.simple.pulsejob.transport.metadata.LogMessage;

import java.util.List;

/**
 * 任务日志监听器接口.
 *
 * <p>在 scheduler 模块定义，由 business 模块提供不同实现：</p>
 * <ul>
 *   <li>数据库存储</li>
 *   <li>本地文件存储</li>
 *   <li>推送到 MQ/ES 等</li>
 * </ul>
 */
public interface JobLogListener {

    /**
     * 处理单条日志
     *
     * @param logMessage 日志消息
     */
    default void onLog(LogMessage logMessage) {
        // 默认空实现，子类可选择实现
    }

    /**
     * 处理批量日志
     *
     * @param logs 日志列表
     */
    default void onBatchLog(List<LogMessage> logs) {
        // 默认逐条调用 onLog
        for (LogMessage log : logs) {
            onLog(log);
        }
    }

    /**
     * 获取监听器优先级（数值越小优先级越高）
     *
     * @return 优先级，默认 100
     */
    default int getOrder() {
        return 100;
    }

    /**
     * 是否支持异步执行
     *
     * @return true-异步执行，false-同步执行
     */
    default boolean isAsync() {
        return true;
    }
}

