package com.simple.pulsejob.admin.scheduler;

import com.simple.pulsejob.admin.common.model.entity.JobInfo;
import com.simple.pulsejob.admin.common.model.enums.DispatchTypeEnum;
import com.simple.pulsejob.admin.common.model.enums.InvokeStrategyEnum;
import com.simple.pulsejob.admin.common.model.enums.LoadBalanceTypeEnum;
import com.simple.pulsejob.admin.common.model.enums.ScheduleTypeEnum;
import com.simple.pulsejob.admin.common.model.enums.SerializerTypeEnum;
import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.JResponse;
import com.simple.pulsejob.transport.channel.JChannel;
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
    private ScheduleTypeEnum scheduleType;

    private DispatchTypeEnum dispatchType;

    private LoadBalanceTypeEnum loadBalanceType;

    private SerializerTypeEnum serializerType;

    /** 集群调用策略 */
    private InvokeStrategyEnum invokeStrategy;

    private int retries;

    /** 超时时间（秒） */
    private int timeoutSeconds;

    // ==================== 运行时状态（调度过程中动态设置） ====================

    /** 当前选中的通道（dispatch 时设置） */
    private JChannel channel;

    /** 当前请求（transport 时设置） */
    private JRequest request;

    /** 响应（执行完成时设置） */
    private JResponse response;

    // ==================== 结果 ====================

    private Object result;

    private Throwable error;

    // ==================== 静态工厂方法 ====================

    /**
     * 从 ScheduleConfig 创建调度上下文
     *
     * @param config 调度配置
     * @return ScheduleContext
     */
    public static ScheduleContext of(ScheduleConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("ScheduleConfig cannot be null");
        }

        ScheduleContext context = new ScheduleContext();

        // 任务基本信息
        context.setJobId(config.getJobId());
        context.setJobHandler(config.getJobHandler());
        context.setJobParams(config.getJobParams());

        // 执行器信息
        context.setExecutorKey(config.getExecutorKey());

        // 调度配置
        context.setScheduleType(config.getScheduleType());
        context.setScheduleExpression(config.getScheduleExpression());
        
        // 分发与负载均衡（使用配置值，如果为空则使用默认值）
        if (config.getDispatchType() != null) {
            context.setDispatchType(config.getDispatchType());
        }
        if (config.getLoadBalanceType() != null) {
            context.setLoadBalanceType(config.getLoadBalanceType());
        }
        if (config.getSerializerType() != null) {
            context.setSerializerType(config.getSerializerType());
        }
        if (config.getInvokeStrategy() != null) {
            context.setInvokeStrategy(config.getInvokeStrategy());
        }

        // 执行配置
        context.setRetries(config.getRetries() > 0 ? config.getRetries() : 1);

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

        // 任务基本信息
        context.setJobId(jobInfo.getId());
        context.setExecutorId(jobInfo.getExecutorId());
        context.setJobHandler(jobInfo.getJobHandler());
        context.setJobParams(jobInfo.getJobParams());

        // 调度配置
        context.setScheduleType(jobInfo.getScheduleType());
        context.setScheduleExpression(jobInfo.getScheduleRate());
        context.setDispatchType(jobInfo.getDispatchType());
        context.setLoadBalanceType(jobInfo.getLoadBalanceType());
        context.setSerializerType(jobInfo.getSerializerType());
        context.setRetries(jobInfo.getMaxRetryTimes() != null ? jobInfo.getMaxRetryTimes() : 1);
        context.setTimeoutSeconds(jobInfo.getTimeoutSeconds() != null ? jobInfo.getTimeoutSeconds() : 60);

        return context;
    }

    // ==================== 构造函数 ====================

    public ScheduleContext() {
        // 默认构造函数，设置默认值
        this.dispatchType = DispatchTypeEnum.ROUND;
        this.loadBalanceType = LoadBalanceTypeEnum.RANDOM;
        this.serializerType = SerializerTypeEnum.JAVA;
        this.invokeStrategy = InvokeStrategyEnum.getDefault();
        this.retries = 1;
        this.timeoutSeconds = 60;
    }

    public ScheduleContext(String executorName) {
        this();
        this.executorKey = ExecutorKey.of(executorName);
    }

    /**
     * 完整构造函数（推荐）
     */
    public ScheduleContext(String executorName, Integer jobId, Integer executorId) {
        this(executorName);
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

