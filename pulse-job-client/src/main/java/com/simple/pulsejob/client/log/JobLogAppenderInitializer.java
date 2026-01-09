package com.simple.pulsejob.client.log;

import javax.annotation.PreDestroy;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.Iterator;

/**
 * Job Log Appender 初始化器.
 *
 * <p>应用启动后自动注册 {@link JobLogAppender} 到 ROOT logger。</p>
 * <p>支持保留 ANSI 颜色码，便于终端和前端彩色展示。</p>
 *
 * <h3>配置项</h3>
 * <pre>
 * pulse-job:
 *   log:
 *     pattern: ""           # 自定义 pattern（留空则自动检测）
 *     color-enabled: true   # 是否启用颜色（默认 true）
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobLogAppenderInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private static final String APPENDER_NAME = "PULSE_JOB_LOG";

    private final JobLogSender jobLogSender;

    /**
     * 可通过配置覆盖 pattern
     */
    @Value("${pulse-job.log.pattern:}")
    private String configuredPattern;

    /**
     * 是否启用颜色输出（默认启用）
     */
    @Value("${pulse-job.log.color-enabled:true}")
    private boolean colorEnabled;

    private JobLogAppender jobLogAppender;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);

        // 检测控制台 appender 的 pattern
        String pattern = detectPattern(rootLogger);
        log.debug("JobLogAppender 使用 pattern: {}, colorEnabled: {}", pattern, colorEnabled);

        // 创建并配置 appender
        jobLogAppender = new JobLogAppender();
        jobLogAppender.setName(APPENDER_NAME);
        jobLogAppender.setJobLogSender(jobLogSender);
        jobLogAppender.setPattern(pattern);
        jobLogAppender.setColorEnabled(colorEnabled);
        jobLogAppender.setContext(loggerContext);
        jobLogAppender.start();

        rootLogger.addAppender(jobLogAppender);
        log.info("JobLogAppender 已注册到 ROOT logger (colorEnabled={})", colorEnabled);
    }

    @PreDestroy
    public void destroy() {
        if (jobLogAppender != null) {
            jobLogAppender.stop();
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
            rootLogger.detachAppender(jobLogAppender);
            log.debug("JobLogAppender 已从 ROOT logger 移除");
        }
    }

    /**
     * 检测 pattern（优先使用配置，其次从 ConsoleAppender 获取）
     */
    private String detectPattern(Logger rootLogger) {
        // 1. 优先使用配置的 pattern
        if (configuredPattern != null && !configuredPattern.isBlank()) {
            return configuredPattern;
        }

        // 2. 尝试从 ConsoleAppender 获取
        Iterator<Appender<ch.qos.logback.classic.spi.ILoggingEvent>> appenders = rootLogger.iteratorForAppenders();
        while (appenders.hasNext()) {
            Appender<?> appender = appenders.next();
            if (appender instanceof ConsoleAppender<?> consoleAppender) {
                if (consoleAppender.getEncoder() instanceof PatternLayoutEncoder encoder) {
                    String pattern = encoder.getPattern();
                    if (pattern != null && !pattern.isBlank()) {
                        log.debug("从 ConsoleAppender '{}' 检测到 pattern", consoleAppender.getName());
                        return pattern;
                    }
                }
            }
        }

        // 3. 使用默认 pattern
        log.debug("未检测到 ConsoleAppender pattern，使用默认值");
        return JobLogAppender.DEFAULT_PATTERN;
    }
}
