package com.simple.pulsejob.admin.common.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public enum ScheduleTypeEnum {

    /** CRON 表达式调度 */
    CRON("cron", 1),
    
    /** 固定频率调度（单位：秒） */
    FIXED_RATE("fixed_rate", 2),
    
    /** 固定延迟调度（单位：秒） */
    FIXED_DELAY("fixed_delay", 3),
    
    /** API 触发（手动触发，不自动调度） */
    API("api", 4);

    private final String name;

    private final Integer code;

    public static ScheduleTypeEnum fromCode(Integer type) {
        for (ScheduleTypeEnum e : values()) {
            if (e.code.equals(type)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown CornTypeEnum type: " + type);
    }
}
