package com.simple.pulsejob.admin.common.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 任务实例状态枚举.
 */
@Getter
@RequiredArgsConstructor
public enum JobInstanceStatus {

    /** 待执行 - 已创建实例，等待调度 */
    PENDING((byte) 0, "待发送"),

    /** 已发送 - 已发送到执行器，等待响应 */
    TRANSPORTED((byte) 1, "已发送"),

    TRANSPORT_FAILED((byte) 2, "发送失败"),

    /** 执行中 - 执行器正在执行 */
    RUNNING((byte) 3, "执行中"),

    /** 成功 - 执行成功 */
    SUCCESS((byte) 4, "成功"),

    /** 失败 - 执行失败 */
    FAILED((byte) 5, "失败"),

    /** 超时 - 执行超时 */
    TIMEOUT((byte) 6, "超时"),

    /** 取消 - 任务被取消 */
    CANCELLED((byte) 7, "已取消");

    private final byte value;
    private final String desc;

    public static JobInstanceStatus of(byte value) {
        for (JobInstanceStatus status : values()) {
            if (status.value == value) {
                return status;
            }
        }
        return null;
    }

    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED || this == TIMEOUT || this == CANCELLED;
    }
}

