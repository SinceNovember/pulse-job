package com.simple.pulsejob.admin.common.model.param;

import com.simple.pulsejob.admin.common.model.base.PageQuery;
import com.simple.pulsejob.admin.common.model.enums.ScheduleTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 任务信息查询参数
 * AI Generated
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class JobInfoQuery extends PageQuery {

    /**
     * 任务处理器名称（模糊匹配）
     */
    private String jobHandler;

    /**
     * 任务描述（模糊匹配）
     */
    private String description;

    /**
     * 执行器ID
     */
    private Integer executorId;

    /**
     * 调度类型
     */
    private ScheduleTypeEnum scheduleType;

    /**
     * 任务状态: 0-禁用, 1-启用
     */
    private Integer status;

}
