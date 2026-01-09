package com.simple.pulsejob.client.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.simple.pulsejob.client.context.JobContextHolder;
import com.simple.pulsejob.transport.metadata.LogMessage;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Pulse-Job 日志 Appender.
 *
 * <p>拦截带有任务上下文（instanceId）的日志，通过 Netty 发送到 Admin。</p>
 * <p>保留 ANSI 颜色码，支持终端和前端彩色展示。</p>
 *
 * <h3>颜色处理策略</h3>
 * <ul>
 *   <li>保留原始 ANSI 码，终端可直接显示颜色</li>
 *   <li>前端可用 ansi-to-html 库转换展示</li>
 * </ul>
 */
public class JobLogAppender extends AppenderBase<ILoggingEvent> {

    /**
     * 默认 pattern（带颜色，与 Spring Boot 控制台风格一致）
     */
    public static final String DEFAULT_PATTERN = 
            "%d{yyyy-MM-dd HH:mm:ss.SSS} %clr(%5p){} %clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} : %m%n";

    /**
     * 无颜色 pattern（降级使用）
     */
    public static final String PLAIN_PATTERN = 
            "%d{yyyy-MM-dd HH:mm:ss.SSS} %5p [%15.15t] %-40.40logger{39} : %m%n";

    @Getter
    @Setter
    private String pattern = DEFAULT_PATTERN;

    /**
     * 是否启用颜色输出（默认启用）
     */
    @Getter
    @Setter
    private boolean colorEnabled = true;

    @Setter
    private JobLogSender jobLogSender;

    private PatternLayoutEncoder encoder;

    @Override
    public void start() {
        if (encoder == null) {
            encoder = new PatternLayoutEncoder();
            encoder.setContext(getContext());
            // 根据配置决定是否使用带颜色的 pattern
            String effectivePattern = colorEnabled ? pattern : stripColorFromPattern(pattern);
            encoder.setPattern(effectivePattern);
            encoder.setCharset(StandardCharsets.UTF_8);
            encoder.start();
        }
        super.start();
    }

    @Override
    public void stop() {
        if (encoder != null) {
            encoder.stop();
        }
        super.stop();
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (jobLogSender == null) {
            return;
        }

        Long instanceId = JobContextHolder.getInstanceId();
        if (instanceId == null) {
            return; // 仅转发带任务上下文的日志
        }

        LogMessage logMessage = new LogMessage();
        try {
            logMessage.setInstanceId(instanceId);
        } catch (NumberFormatException ignored) {
            return;
        }

        logMessage.setLevel(mapLevel(event.getLevel()));
        logMessage.setJobId(JobContextHolder.getJobId());
        // 保留完整格式化内容（含 ANSI 码）
        logMessage.setContent(formatMessage(event));
        jobLogSender.sendAsync(logMessage);
    }

    /**
     * 格式化日志消息（保留 ANSI 颜色码）
     */
    private String formatMessage(ILoggingEvent event) {
        if (encoder != null) {
            byte[] encoded = encoder.encode(event);
            String formatted = new String(encoded, StandardCharsets.UTF_8);
            return formatted.stripTrailing();
        }
        return event.getFormattedMessage();
    }

    /**
     * 从 pattern 中移除颜色指令（用于禁用颜色时）
     */
    private String stripColorFromPattern(String originalPattern) {
        if (originalPattern == null || originalPattern.isBlank()) {
            return PLAIN_PATTERN;
        }

        if (!originalPattern.contains("%clr")) {
            return originalPattern;
        }

        String cleaned = originalPattern;
        // %clr(%xxx){color} -> %xxx
        cleaned = cleaned.replaceAll("%clr\\(([^)]*)\\)\\{[^}]*\\}", "$1");
        // %clr(%xxx) -> %xxx
        cleaned = cleaned.replaceAll("%clr\\(([^)]*)\\)", "$1");
        // 清理 Spring Boot 特有转换词
        cleaned = cleaned.replace("%wEx", "%ex");
        cleaned = cleaned.replace("%applicationName", "");

        return cleaned.isBlank() ? PLAIN_PATTERN : cleaned;
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
