package com.simple.pulsejob.admin.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RegisterTypeEnum {
    
    AUTO(0, "自动注册"),
    MANUAL(1, "手动录入");
    
    private final Integer code;
    private final String desc;

    public static RegisterTypeEnum fromCode(Integer type) {
        for (RegisterTypeEnum e : values()) {
            if (e.code.equals(type)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown CornTypeEnum type: " + type);
    }
}