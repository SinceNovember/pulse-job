package com.simple.pulsejob.client.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.simple.pulsejob.transport.metadata.LogMessage;
import lombok.Setter;

/**
 * 将带 taskId(instanceId) 的日志推送到 admin。
 * 
 * <p>支持两种发送模式：</p>
 * <ul>
 *   <li>单条发送 - 使用 {@link JobLogSenderTest}</li>
 *   <li>批量发送 - 使用 {@link JobLogSender}（推荐，更高效）</li>
 * </ul>
 */
public class CustomLogAppender extends AppenderBase<ILoggingEvent> {

    @Setter
    private JobLogSender jobLogSender;

    @Override
    protected void append(ILoggingEvent event) {
        String taskId = event.getMDCPropertyMap().get(LogMDCScope.TASK_ID);
        if (taskId == null) {
            return; // 仅转发带任务上下文的日志
        }

        LogMessage logMessage = new LogMessage();
        try {
            logMessage.setInstanceId(Long.parseLong(taskId));
        } catch (NumberFormatException ignored) {
            return;
        }
        logMessage.setLevel(mapLevel(event.getLevel()));
        logMessage.setContent(event.getFormattedMessage());
        jobLogSender.sendAsync(logMessage);

    }

    private LogMessage.LogLevel mapLevel(Level level) {
        if (level == Level.ERROR) {
            return LogMessage.LogLevel.ERROR;
        }
        if (level == Level.WARN) {
            return LogMessage.LogLevel.WARN;
        }
        if (level == Level.DEBUG || level == Level.TRACE) {
            return LogMessage.LogLevel.DEBUG;
        }
        return LogMessage.LogLevel.INFO;
    }
}
