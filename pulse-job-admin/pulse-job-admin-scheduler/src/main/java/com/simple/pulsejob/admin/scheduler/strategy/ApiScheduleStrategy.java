package com.simple.pulsejob.admin.scheduler.strategy;

import com.simple.pulsejob.admin.common.model.enums.ScheduleTypeEnum;
import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * API 触发调度策略
 * <p>
 * 不进行自动调度，仅支持通过 API 手动触发执行
 * </p>
 *
 * @author pulse
 */
@Slf4j
@Component
public class ApiScheduleStrategy extends AbstractScheduleStrategy {

    @Override
    public ScheduleTypeEnum getType() {
        return ScheduleTypeEnum.API;
    }

    @Override
    public LocalDateTime calculateNextExecuteTime(ScheduleContext context, LocalDateTime lastExecuteTime) {
        // API 类型不自动调度，返回 null
        log.debug("API 类型任务不自动调度");
        return null;
    }

    @Override
    public boolean needAutoSchedule() {
        // API 类型不需要自动调度
        return false;
    }

    @Override
    public boolean validateExpression(String expression) {
        // API 类型不需要表达式
        return true;
    }

    @Override
    public String getDescription(String expression) {
        return "仅 API 手动触发";
    }
}

