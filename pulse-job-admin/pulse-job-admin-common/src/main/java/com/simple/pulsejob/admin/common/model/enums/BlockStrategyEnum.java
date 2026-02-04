package com.simple.pulsejob.admin.common.model.enums;

/**
 * 阻塞处理策略枚举
 * AI Generated
 */
public enum BlockStrategyEnum {
    
    /**
     * 单机串行 - 排队等待执行
     */
    SERIAL_EXECUTION,
    
    /**
     * 丢弃后续调度 - 丢弃本次调度
     */
    DISCARD_LATER,
    
    /**
     * 覆盖之前调度 - 终止正在执行的任务，执行本次
     */
    COVER_EARLY
}
