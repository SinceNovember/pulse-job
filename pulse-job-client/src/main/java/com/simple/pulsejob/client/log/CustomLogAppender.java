package com.simple.pulsejob.client.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.simple.pulsejob.transport.metadata.LogMessage;
import lombok.Setter;

/**
 * 将带 taskId(invokeId) 的日志推送到 admin。
 */
public class CustomLogAppender extends AppenderBase<ILoggingEvent> {

    @Setter
    private JobLogSender jobLogSender;

    @Override
    protected void append(ILoggingEvent event) {
        if (jobLogSender == null) {
            return;
        }

        String taskId = event.getMDCPropertyMap().get(LogMDCScope.TASK_ID);
        if (taskId == null) {
            return; // 仅转发带任务上下文的日志
        }

        LogMessage logMessage = new LogMessage();
        try {
            logMessage.setInvokeId(Long.parseLong(taskId));
        } catch (NumberFormatException ignored) {
            return;
        }
        logMessage.setLevel(mapLevel(event.getLevel()));
        logMessage.setContent(event.getFormattedMessage());
        // timestamp/sequence 由 JobLogSender 填充

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
