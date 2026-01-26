package com.simple.pulsejob.admin.scheduler;

import com.simple.pulsejob.admin.common.model.dto.JobInfoWithExecutorDTO;
import com.simple.pulsejob.admin.common.model.entity.JobInfo;
import com.simple.pulsejob.admin.common.model.enums.DispatchTypeEnum;
import com.simple.pulsejob.admin.common.model.enums.InvokeStrategyEnum;
import com.simple.pulsejob.admin.common.model.enums.LoadBalanceTypeEnum;
import com.simple.pulsejob.admin.common.model.enums.ScheduleTypeEnum;
import com.simple.pulsejob.admin.common.model.enums.SerializerTypeEnum;
import com.simple.pulsejob.common.util.StringUtil;
import com.simple.pulsejob.transport.metadata.ExecutorKey;
import lombok.Data;

/**
 * 调度上下文.
 *
 * <p>包含调度所需的配置信息和运行时状态</p>
 */
@Data
public class ScheduleContext {

    // ==================== 执行器相关 ====================

    /** 执行器标识（用于路由） */
    private ExecutorKey executorKey;

    /** 执行器ID（数据库关联） */
    private Integer executorId;

    // ==================== 任务基本信息 ====================

    /** 任务ID */
    private Integer jobId;

    /** 实例ID（运行时生成） */
    private Long instanceId;

    /** 任务处理器名称 */
    private String jobHandler;

    /** 任务参数 */
    private String jobParams;

    // ==================== 调度配置 ====================

    /** 调度表达式（CRON表达式或固定间隔秒数） */
    private String scheduleExpression;

    /** 调度类型：CRON、FIXED_RATE、FIXED_DELAY、API */
    private ScheduleTypeEnum scheduleType;

    /** 分发类型：ROUND（单播）、BROADCAST（广播） */
    private DispatchTypeEnum dispatchType;

    /** 负载均衡类型 */
    private LoadBalanceTypeEnum loadBalanceType;

    /** 序列化类型 */
    private SerializerTypeEnum serializerType;

    /** 集群调用策略：FAIL_FAST、FAIL_OVER、FAIL_SAFE */
    private InvokeStrategyEnum invokeStrategy;

    /** 最大重试次数 */
    private int maxRetries;

    /** 超时时间（秒） */
    private int timeoutSeconds;

    // ==================== 静态工厂方法 ====================

    /**
     * 从 JobInfo 实体创建调度上下文
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
        context.setMaxRetries(jobInfo.getMaxRetryTimes() != null ? jobInfo.getMaxRetryTimes() : 1);
        context.setTimeoutSeconds(jobInfo.getTimeoutSeconds() != null ? jobInfo.getTimeoutSeconds() : 60);

        return context;
    }

    /**
     * 从 JobInfo 创建调度上下文，可覆盖参数
     *
     * @param jobInfo        任务实体
     * @param overrideParams 覆盖的参数（可为 null）
     * @return ScheduleContext
     */
    public static ScheduleContext of(JobInfo jobInfo, String overrideParams) {
        ScheduleContext context = of(jobInfo);
        if (StringUtil.isNotBlank(overrideParams)) {
            context.setJobParams(overrideParams);
        }
        return context;
    }

    /**
     * 从 JobInfoWithExecutorDTO 创建调度上下文（推荐）
     *
     * @param jobDetail            包含 JobInfo 和 executorName 的 DTO
     * @param overrideParams 覆盖的参数（可为 null）
     * @return ScheduleContext
     */
    public static ScheduleContext of(JobInfoWithExecutorDTO jobDetail, String overrideParams) {
        if (jobDetail == null || jobDetail.getJobInfo() == null) {
            throw new IllegalArgumentException("jobDetail cannot be null");
        }

        ScheduleContext context = of(jobDetail.getJobInfo(), overrideParams);

        // 设置 ExecutorKey
        if (StringUtil.isNotBlank(jobDetail.getExecutorName())) {
            context.setExecutorKey(ExecutorKey.of(jobDetail.getExecutorName()));
        }

        return context;
    }

    /**
     * 从 JobInfoWithExecutorDTO 创建调度上下文
     *
     * @param dto 包含 JobInfo 和 executorName 的 DTO
     * @return ScheduleContext
     */
    public static ScheduleContext of(JobInfoWithExecutorDTO dto) {
        return of(dto, null);
    }

    // ==================== 构造函数 ====================

    public ScheduleContext() {
        // 默认值
        this.dispatchType = DispatchTypeEnum.ROUND;
        this.loadBalanceType = LoadBalanceTypeEnum.RANDOM;
        this.serializerType = SerializerTypeEnum.JAVA;
        this.invokeStrategy = InvokeStrategyEnum.getDefault();
        this.maxRetries = 1;
        this.timeoutSeconds = 60;
    }

    public ScheduleContext(String executorName) {
        this();
        this.executorKey = ExecutorKey.of(executorName);
    }

    public ScheduleContext(String executorName, Integer jobId, Integer executorId) {
        this(executorName);
        this.jobId = jobId;
        this.executorId = executorId;
    }

    // ==================== 便捷方法 ====================

    /**
     * 获取重试次数（兼容旧 API）
     */
    public int getRetries() {
        return maxRetries;
    }

    /**
     * 设置重试次数（兼容旧 API）
     */
    public void setRetries(int retries) {
        this.maxRetries = retries;
    }
}

