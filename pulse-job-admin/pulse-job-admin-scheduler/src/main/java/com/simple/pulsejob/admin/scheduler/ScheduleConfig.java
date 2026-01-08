package com.simple.pulsejob.admin.scheduler;

import com.simple.plusejob.serialization.SerializerType;
import com.simple.pulsejob.admin.scheduler.cluster.ClusterInvoker;
import com.simple.pulsejob.admin.scheduler.dispatch.Dispatcher;
import com.simple.pulsejob.admin.scheduler.load.balance.LoadBalancer;
import com.simple.pulsejob.admin.scheduler.strategy.ScheduleStrategy;
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
    private ScheduleStrategy.Type scheduleType;

    private Dispatcher.Type dispatchType;

    private LoadBalancer.Type loadBalanceType;

    private ClusterInvoker.Strategy invokeStrategy;

    private SerializerType serializerType;

    private boolean sync;

    private int retries;
}
