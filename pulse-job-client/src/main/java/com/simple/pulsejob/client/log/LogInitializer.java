package com.simple.pulsejob.client.log;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;

public class LogInitializer {

    public static void initTaskLogAppender() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger root = loggerContext.getLogger("ROOT");
        PulseJobLogAppender appender = new PulseJobLogAppender();
        appender.setContext(loggerContext);
        appender.start();

        root.addAppender(appender);
    }
}
