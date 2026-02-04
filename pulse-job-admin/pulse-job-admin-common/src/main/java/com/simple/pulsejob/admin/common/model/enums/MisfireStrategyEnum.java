package com.simple.pulsejob.admin.common.model.enums;

/**
 * 调度过期策略枚举
 * AI Generated
 */
public enum MisfireStrategyEnum {
    
    /**
     * 忽略 - 不执行过期的调度
     */
    DO_NOTHING,
    
    /**
     * 立即执行一次 - 过期后立即执行一次
     */
    FIRE_ONCE_NOW
}
