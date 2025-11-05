package com.simple.pulsejob.admin.common.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public enum ScheduleTypeEnum {

    CRON("cron", 1);

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
