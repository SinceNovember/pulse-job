package com.simple.pulsejob.client.log;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomLogAppenderInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private final JobLogSender jobLogSender;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger root = loggerContext.getLogger("ROOT");
        CustomLogAppender appender = new CustomLogAppender();
        appender.setJobLogSender(jobLogSender);
        appender.setContext(loggerContext);
        appender.start();

        root.addAppender(appender);
    }
}
