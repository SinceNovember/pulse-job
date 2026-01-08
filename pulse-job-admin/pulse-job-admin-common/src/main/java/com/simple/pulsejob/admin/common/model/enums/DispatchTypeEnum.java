package com.simple.pulsejob.admin.common.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 分发类型枚举
 *
 * @author pulse
 */
@Getter
@AllArgsConstructor
public enum DispatchTypeEnum {

    /** 单播（轮询选择一个执行器） */
    ROUND("round", 1),

    /** 广播（所有执行器都执行） */
    BROADCAST("broadcast", 2);

    private final String name;
    private final Integer code;

    public static DispatchTypeEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (DispatchTypeEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown DispatchTypeEnum code: " + code);
    }

    public static DispatchTypeEnum fromName(String name) {
        if (name == null) {
            return null;
        }
        for (DispatchTypeEnum e : values()) {
            if (e.name.equalsIgnoreCase(name)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown DispatchTypeEnum name: " + name);
    }
}

