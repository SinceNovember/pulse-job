package com.simple.pulsejob.admin.common.model.enums;

import com.simple.pulsejob.transport.metadata.LogMessage;
import lombok.Getter;

/**
 * 日志级别枚举.
 */
@Getter
public enum LogLevelEnum {

    DEBUG("DEBUG", "调试"),
    INFO("INFO", "信息"),
    WARN("WARN", "警告"),
    ERROR("ERROR", "错误");

    private final String code;
    private final String desc;

    LogLevelEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 从 transport 层的 LogLevel 转换
     */
    public static LogLevelEnum fromTransportLevel(LogMessage.LogLevel level) {
        if (level == null) {
            return INFO;
        }
        return switch (level) {
            case DEBUG -> DEBUG;
            case INFO -> INFO;
            case WARN -> WARN;
            case ERROR -> ERROR;
        };
    }

    /**
     * 根据code获取枚举
     */
    public static LogLevelEnum fromCode(String code) {
        if (code == null) {
            return INFO;
        }
        for (LogLevelEnum level : values()) {
            if (level.code.equalsIgnoreCase(code)) {
                return level;
            }
        }
        return INFO;
    }
}

