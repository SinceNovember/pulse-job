package com.simple.pulsejob.admin.scheduler.strategy;

import com.simple.pulsejob.admin.common.model.entity.JobInfo;
import com.simple.pulsejob.admin.common.model.enums.ScheduleTypeEnum;
import com.simple.pulsejob.admin.scheduler.ScheduleContext;

import java.time.LocalDateTime;

/**
 * 调度策略接口
 * <p>
 * 定义不同调度类型的行为规范，支持：
 * <ul>
 *   <li>CRON - CRON 表达式调度</li>
 *   <li>FIXED_RATE - 固定频率调度</li>
 *   <li>FIXED_DELAY - 固定延迟调度</li>
 *   <li>API - API 手动触发</li>
 * </ul>
 * </p>
 *
 * @author pulse
 */
public interface ScheduleStrategy {

    /**
     * 获取策略支持的调度类型
     *
     * @return 调度类型
     */
    ScheduleTypeEnum getType();

    /**
     * 计算下次执行时间
     *
     * @param context 调度上下文
     * @param lastExecuteTime 上次执行时间（可为null表示首次调度）
     * @return 下次执行时间，null 表示不需要下次调度
     */
    LocalDateTime calculateNextExecuteTime(ScheduleContext context, LocalDateTime lastExecuteTime);

    /**
     * 计算下次执行时间（基于 JobInfo）
     *
     * @param jobInfo 任务信息
     * @return 下次执行时间，null 表示不需要下次调度
     */
    LocalDateTime calculateNextExecuteTime(JobInfo jobInfo);

    /**
     * 判断是否需要自动调度
     * <p>
     * API 类型不需要自动调度，需要手动触发
     * </p>
     *
     * @return true-需要自动调度，false-不需要
     */
    default boolean needAutoSchedule() {
        return true;
    }

    /**
     * 验证调度表达式是否合法
     *
     * @param expression 调度表达式
     * @return true-合法，false-不合法
     */
    boolean validateExpression(String expression);

    /**
     * 获取调度间隔描述
     *
     * @param expression 调度表达式
     * @return 描述信息
     */
    String getDescription(String expression);
}
