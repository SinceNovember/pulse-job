package com.simple.pulsejob.admin.scheduler;

import com.simple.pulsejob.admin.common.model.enums.DispatchTypeEnum;
import com.simple.pulsejob.admin.common.model.enums.LoadBalanceTypeEnum;
import com.simple.pulsejob.admin.common.model.enums.ScheduleTypeEnum;
import com.simple.pulsejob.admin.common.model.enums.SerializerTypeEnum;
import com.simple.pulsejob.admin.scheduler.cluster.ClusterInvoker;
import com.simple.pulsejob.transport.metadata.ExecutorKey;
import lombok.Data;

@Data
public class ScheduleConfig {

    // ==================== 执行器相关 ====================

    private ExecutorKey executorKey;

    // ==================== 任务基本信息 ====================

    private Integer jobId;

    /** 任务处理器名称 */
    private String jobHandler;

    /** 任务参数 */
    private String jobParams;

    // ==================== 调度配置 ====================

    /** 调度表达式（CRON表达式或固定间隔秒数） */
    private String scheduleExpression;

    /** 调度类型：CRON、FIXED_RATE、FIXED_DELAY、API */
    private ScheduleTypeEnum scheduleType;

    private DispatchTypeEnum dispatchType;

    private LoadBalanceTypeEnum loadBalanceType;

    private ClusterInvoker.Strategy invokeStrategy;

    private SerializerTypeEnum serializerType;

    private boolean sync;

    private int retries;
}
