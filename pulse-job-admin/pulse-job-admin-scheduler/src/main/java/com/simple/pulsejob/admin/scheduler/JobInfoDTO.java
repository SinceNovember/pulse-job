package com.simple.pulsejob.admin.scheduler;

import com.simple.pulsejob.admin.common.model.entity.JobInfo;
import com.simple.pulsejob.admin.common.model.enums.DispatchTypeEnum;
import com.simple.pulsejob.admin.common.model.enums.LoadBalanceTypeEnum;
import com.simple.pulsejob.admin.common.model.enums.ScheduleTypeEnum;
import com.simple.pulsejob.admin.common.model.enums.SerializerTypeEnum;
import com.simple.pulsejob.admin.scheduler.cluster.ClusterInvoker;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务信息 DTO
 * <p>
 * 用于封装调度所需的任务信息，可从 {@link JobInfo} 实体转换而来
 * </p>
 *
 * @author pulse
 */
@Data
@Builder
public class JobInfoDTO {

    // ==================== 任务基本信息 ====================

    /** 任务ID */
    private Long jobId;

    /** 任务处理器名称 */
    private String jobHandler;

    /** 任务参数 */
    private String jobParams;

    // ==================== 执行器信息 ====================

    /** 执行器ID */
    private Long executorId;

    /** 执行器名称 */
    private String executorName;

    // ==================== 调度配置 ====================

    /** 调度类型 */
    private ScheduleTypeEnum scheduleType;

    /** 调度表达式（CRON表达式或固定间隔秒数） */
    private String scheduleExpression;

    /** 下次执行时间 */
    private LocalDateTime nextExecuteTime;

    /** 上次执行时间 */
    private LocalDateTime lastExecuteTime;

    // ==================== 执行配置 ====================

    /** 分发类型 */
    @Builder.Default
    private DispatchTypeEnum dispatchType = DispatchTypeEnum.ROUND;

    /** 负载均衡类型 */
    @Builder.Default
    private LoadBalanceTypeEnum loadBalanceType = LoadBalanceTypeEnum.RANDOM;

    /** 集群调用策略 */
    @Builder.Default
    private ClusterInvoker.Strategy invokeStrategy = ClusterInvoker.Strategy.FAIL_FAST;

    /** 序列化类型 */
    @Builder.Default
    private SerializerTypeEnum serializerType = SerializerTypeEnum.JAVA;

    /** 是否同步调用 */
    @Builder.Default
    private boolean sync = false;

    /** 重试次数 */
    @Builder.Default
    private int retries = 1;

    /** 超时时间（秒） */
    @Builder.Default
    private int timeoutSeconds = 60;

    // ==================== 静态工厂方法 ====================

    /**
     * 从 JobInfo 实体创建 DTO
     *
     * @param jobInfo 任务实体
     * @return JobInfoDTO
     */
    public static JobInfoDTO from(JobInfo jobInfo) {
        if (jobInfo == null) {
            return null;
        }

        return JobInfoDTO.builder()
                .jobId(jobInfo.getId() != null ? Long.valueOf(jobInfo.getId()) : null)
                .jobHandler(jobInfo.getJobHandler())
                .jobParams(jobInfo.getJobParams())
                .executorId(jobInfo.getExecutorId() != null ? Long.valueOf(jobInfo.getExecutorId()) : null)
                .scheduleType(jobInfo.getScheduleType())
                .scheduleExpression(jobInfo.getScheduleRate())
                .nextExecuteTime(jobInfo.getNextExecuteTime())
                .lastExecuteTime(jobInfo.getLastExecuteTime())
                .dispatchType(jobInfo.getDispatchType())
                .loadBalanceType(jobInfo.getLoadBalanceType())
                .serializerType(jobInfo.getSerializerType())
                .retries(jobInfo.getMaxRetryTimes() != null ? jobInfo.getMaxRetryTimes() : 1)
                .timeoutSeconds(jobInfo.getTimeoutSeconds() != null ? jobInfo.getTimeoutSeconds() : 60)
                .build();
    }

    /**
     * 快速创建用于手动触发的 DTO
     *
     * @param executorName 执行器名称
     * @param jobId        任务ID
     * @param executorId   执行器ID
     * @param jobHandler   处理器名称
     * @param jobParams    任务参数
     * @return JobInfoDTO
     */
    public static JobInfoDTO ofTrigger(String executorName, Long jobId, Long executorId,
                                        String jobHandler, String jobParams) {
        return JobInfoDTO.builder()
                .executorName(executorName)
                .jobId(jobId)
                .executorId(executorId)
                .jobHandler(jobHandler)
                .jobParams(jobParams)
                .scheduleType(ScheduleTypeEnum.API)
                .build();
    }
}
