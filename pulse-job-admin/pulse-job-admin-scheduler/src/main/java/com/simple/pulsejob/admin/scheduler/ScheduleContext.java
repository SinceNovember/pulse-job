package com.simple.pulsejob.admin.scheduler;

import com.simple.plusejob.serialization.SerializerType;
import com.simple.pulsejob.admin.scheduler.cluster.ClusterInvoker;
import com.simple.pulsejob.admin.scheduler.dispatch.Dispatcher;
import com.simple.pulsejob.admin.scheduler.load.balance.LoadBalancer;
import com.simple.pulsejob.admin.scheduler.strategy.ScheduleStrategy;
import com.simple.pulsejob.transport.metadata.ExecutorKey;
import lombok.Data;

@Data
public class ScheduleContext {

    // ==================== 执行器相关 ====================

    private ExecutorKey executorKey;

    private ClusterInvoker invoker;

    // ==================== 任务实例相关 ====================

    /** 任务ID（JobInfo.id） */
    private Long jobId;

    /** 执行器ID（JobExecutor.id） */
    private Long executorId;

    /** 任务实例ID（JobInstance.id），同时作为 instanceId */
    private Long instanceId;

    // ==================== 调度配置 ====================

    /** 调度类型：CRON、FIXED_RATE、FIXED_DELAY、API */
    private ScheduleStrategy.Type scheduleType;

    /** 调度表达式（CRON表达式或固定间隔秒数） */
    private String scheduleExpression;

    private boolean sync;

    private Dispatcher.Type dispatchType;

    private LoadBalancer.Type loadBalanceType;

    private ClusterInvoker.Strategy invokeStrategy;

    private SerializerType serializerType;

    private int retries;

    // ==================== 结果 ====================

    private Object result;

    private Throwable error;

    // ==================== 构造函数 ====================

    public ScheduleContext() {
        // 默认构造函数
    }

    public ScheduleContext(String executorName, ClusterInvoker invoker, boolean sync) {
        this.executorKey = ExecutorKey.of(executorName);
        this.invoker = invoker;
        this.sync = sync;
        this.dispatchType = Dispatcher.Type.ROUND;
        this.loadBalanceType = LoadBalancer.Type.RANDOM;
        this.serializerType = SerializerType.JAVA;
        this.retries = 1;
    }

    /**
     * 完整构造函数（推荐）
     */
    public ScheduleContext(String executorName, Long jobId, Long executorId,
                           ClusterInvoker invoker, boolean sync) {
        this(executorName, invoker, sync);
        this.jobId = jobId;
        this.executorId = executorId;
    }

    /**
     * 获取 instanceId（即 instanceId）
     */
    public Long getInstanceId() {
        return instanceId;
    }

    /**
     * 判断是否已创建实例
     */
    public boolean hasInstance() {
        return instanceId != null;
    }

    /**
     * 标记执行成功
     */
    public void markSuccess(Object result) {
        this.result = result;
        this.error = null;
    }

    /**
     * 标记执行失败
     */
    public void markFailed(Throwable error) {
        this.error = error;
    }

    public boolean isSuccess() {
        return error == null;
    }
}

