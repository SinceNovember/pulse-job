package com.simple.pulsejob.admin.scheduler;

import com.simple.plusejob.serialization.SerializerType;
import com.simple.pulsejob.admin.common.model.entity.JobInfo;
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

    // ==================== 任务基本信息 ====================

    private Integer jobId;

    private Integer executorId;

    private Long instanceId;

    /** 任务处理器名称 */
    private String jobHandler;

    private String jobParams;

    // ==================== 调度配置 ====================

    /** 调度表达式（CRON表达式或固定间隔秒数） */
    private String scheduleExpression;

    /** 调度类型：CRON、FIXED_RATE、FIXED_DELAY、API */
    private ScheduleStrategy.Type scheduleType;

    private Dispatcher.Type dispatchType;

    private LoadBalancer.Type loadBalanceType;

    private SerializerType serializerType;

    private boolean sync;

    private int retries;

    /** 超时时间（秒） */
    private int timeoutSeconds;

    // ==================== 结果 ====================

    private Object result;

    private Throwable error;

    // ==================== 静态工厂方法 ====================

    public static ScheduleContext of(ScheduleConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("ScheduleConfig cannot be null");
        }

        ScheduleContext context = new ScheduleContext();
        context.setJobId(config.getJobId());
        context.setJobParams(config.getJobParams());
        return context;
    }


        /**
         * 从 JobInfo 实体创建调度上下文（无 invoker）
         *
         * @param jobInfo 任务实体
         * @return ScheduleContext
         */
    public static ScheduleContext of(JobInfo jobInfo) {
        if (jobInfo == null) {
            throw new IllegalArgumentException("JobInfo cannot be null");
        }

        ScheduleContext context = new ScheduleContext();

        // 执行器信息
        context.setExecutorKey(ExecutorKey.of(jobInfo.getExecutorName()));

        // 任务基本信息
        context.setJobId(jobInfo.getId());
        context.setExecutorId(jobInfo.getExecutorId());
        context.setJobHandler(jobInfo.getJobHandler());
        context.setJobParams(jobInfo.getJobParams());

        // 调度配置
        context.setScheduleType(ScheduleStrategy.Type.from(jobInfo.getScheduleType()));
        context.setScheduleExpression(jobInfo.getScheduleRate());
        context.setRetries(jobInfo.getMaxRetryTimes() != null ? jobInfo.getMaxRetryTimes() : 1);
        context.setTimeoutSeconds(jobInfo.getTimeoutSeconds() != null ? jobInfo.getTimeoutSeconds() : 60);

        return context;
    }

    // ==================== 构造函数 ====================

    public ScheduleContext() {
        // 默认构造函数，设置默认值
        this.dispatchType = Dispatcher.Type.ROUND;
        this.loadBalanceType = LoadBalancer.Type.RANDOM;
        this.serializerType = SerializerType.JAVA;
        this.retries = 1;
        this.timeoutSeconds = 60;
    }

    public ScheduleContext(String executorName, ClusterInvoker invoker, boolean sync) {
        this();
        this.executorKey = ExecutorKey.of(executorName);
        this.sync = sync;
    }

    /**
     * 完整构造函数（推荐）
     */
    public ScheduleContext(String executorName, Integer jobId, Integer executorId,
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

