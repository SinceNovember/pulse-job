package com.simple.pulsejob.admin.common.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 负载均衡类型枚举
 *
 * @author pulse
 */
@Getter
@AllArgsConstructor
public enum LoadBalanceTypeEnum {

    /** 轮询 */
    ROUND("round", 1),

    /** 随机 */
    RANDOM("random", 2),

    /** 一致性哈希 */
    CONSISTENT_HASH("consistent_hash", 3),

    /** 最少活跃 */
    LEAST_ACTIVE("least_active", 4);

    private final String name;
    private final Integer code;

    public static LoadBalanceTypeEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (LoadBalanceTypeEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown LoadBalanceTypeEnum code: " + code);
    }

    public static LoadBalanceTypeEnum fromName(String name) {
        if (name == null) {
            return null;
        }
        for (LoadBalanceTypeEnum e : values()) {
            if (e.name.equalsIgnoreCase(name)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown LoadBalanceTypeEnum name: " + name);
    }
}

