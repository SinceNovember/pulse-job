package com.simple.pulsejob.admin.common.model.enums;

/**
 * 集群调用策略枚举.
 *
 * <ul>
 *   <li>FAIL_FAST - 快速失败：只发起一次调用，失败立即报错。适用于非幂等写操作。</li>
 *   <li>FAIL_OVER - 失败重试：失败时自动切换到其他服务器重试。适用于幂等性读操作。</li>
 *   <li>FAIL_SAFE - 失败安全：失败时只打印日志，不抛异常。适用于写入审计日志等操作。</li>
 * </ul>
 *
 * @author pulse
 */
public enum InvokeStrategyEnum {

    /**
     * 快速失败：只发起一次调用，失败立即报错
     */
    FAIL_FAST,

    /**
     * 失败重试：失败时自动切换到其他服务器重试
     */
    FAIL_OVER,

    /**
     * 失败安全：失败时只打印日志，不抛异常
     */
    FAIL_SAFE;

    // FAIL_BACK  - 暂不支持，没想到合适场景
    // FORKING    - 暂不支持，消耗资源太多

    /**
     * 根据名称解析策略
     */
    public static InvokeStrategyEnum parse(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        for (InvokeStrategyEnum s : values()) {
            if (s.name().equalsIgnoreCase(name)) {
                return s;
            }
        }
        return null;
    }

    /**
     * 获取默认策略
     */
    public static InvokeStrategyEnum getDefault() {
        return FAIL_FAST;
    }
}
