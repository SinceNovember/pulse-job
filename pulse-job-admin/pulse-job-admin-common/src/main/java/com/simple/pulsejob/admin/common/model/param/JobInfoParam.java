package com.simple.pulsejob.admin.common.model.param;

import com.simple.pulsejob.admin.common.model.enums.BlockStrategyEnum;
import com.simple.pulsejob.admin.common.model.enums.DispatchTypeEnum;
import com.simple.pulsejob.admin.common.model.enums.GlueTypeEnum;
import com.simple.pulsejob.admin.common.model.enums.LoadBalanceTypeEnum;
import com.simple.pulsejob.admin.common.model.enums.MisfireStrategyEnum;
import com.simple.pulsejob.admin.common.model.enums.ScheduleTypeEnum;
import lombok.Data;

/**
 * 任务信息创建/更新参数
 * AI Generated - 增加前端表单所需字段
 */
@Data
public class JobInfoParam {

    /**
     * 任务处理器名称
     */
    private String jobHandler;

    /**
     * 运行模式
     */
    private GlueTypeEnum glueType;

    /**
     * 调度频率表达式（Cron表达式或毫秒数）
     */
    private String scheduleRate;

    /**
     * 调度类型
     */
    private ScheduleTypeEnum scheduleType;

    /**
     * 执行器ID
     */
    private Integer executorId;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 负责人
     */
    private String owner;

    /**
     * 报警邮件（多个用逗号分隔）
     */
    private String alarmEmail;

    /**
     * 任务参数
     */
    private String jobParams;

    /**
     * 子任务ID（多个用逗号分隔）
     */
    private String childJobId;

    /**
     * 分发类型：ROUND-单播轮询, BROADCAST-广播
     */
    private DispatchTypeEnum dispatchType;

    /**
     * 负载均衡类型/路由策略
     */
    private LoadBalanceTypeEnum loadBalanceType;

    /**
     * 调度过期策略
     */
    private MisfireStrategyEnum misfireStrategy;

    /**
     * 阻塞处理策略
     */
    private BlockStrategyEnum blockStrategy;

    /**
     * 任务超时时间（秒）
     */
    private Integer timeoutSeconds;

    /**
     * 最大重试次数
     */
    private Integer maxRetryTimes;

}
