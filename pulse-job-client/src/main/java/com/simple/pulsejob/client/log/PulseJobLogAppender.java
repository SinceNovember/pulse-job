package com.simple.pulsejob.client.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

public class PulseJobLogAppender extends AppenderBase<ILoggingEvent> {

    private String logDirectory = "D://logs";

    @Override
    protected void append(ILoggingEvent event) {
        try {
            String logMessage = event.getTimeStamp() + " [" + event.getLevel() + "] " + event.getLoggerName() + " - " + event.getFormattedMessage();
            writeToLocalFile(event.getMDCPropertyMap().get("taskId"), logMessage);
        } catch (Exception e) {
            addError("Failed to write log", e);
        }
    }

    private void writeToLocalFile(String taskId, String logMessage) {
        if (taskId == null) return;  // 只处理带有 taskId 的日志

        String logFilePath = logDirectory + "/" + taskId + ".log";
        File file = new File(logFilePath);

        try {
            // 确保目录存在
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }

            // 确保日志文件存在
            if (!file.exists()) {
                file.createNewFile();
            }

            // 追加写入日志
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                writer.write(logMessage + "\n");
            }
        } catch (IOException e) {
            addError("Failed to write log file", e);
        }
    }
}
