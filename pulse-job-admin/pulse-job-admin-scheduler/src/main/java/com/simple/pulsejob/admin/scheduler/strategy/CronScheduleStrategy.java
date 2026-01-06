package com.simple.pulsejob.admin.scheduler.strategy;

import com.simple.pulsejob.admin.common.model.enums.ScheduleTypeEnum;
import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * CRON 表达式调度策略
 *
 * @author pulse
 */
@Slf4j
@Component
public class CronScheduleStrategy extends AbstractScheduleStrategy {

    @Override
    public ScheduleTypeEnum getType() {
        return ScheduleTypeEnum.CRON;
    }

    @Override
    public LocalDateTime calculateNextExecuteTime(ScheduleContext context, LocalDateTime lastExecuteTime) {
        String cronExpression = context.getScheduleExpression();

        if (cronExpression == null || cronExpression.isEmpty()) {
            log.warn("CRON 表达式为空，无法计算下次执行时间");
            return null;
        }

        try {
            CronExpression cron = CronExpression.parse(cronExpression);

            // 基于当前时间或上次执行时间计算下次执行时间
            LocalDateTime baseTime = lastExecuteTime != null ? lastExecuteTime : LocalDateTime.now();
            LocalDateTime nextTime = cron.next(baseTime);

            log.debug("CRON[{}] 基于 {} 计算下次执行时间: {}", cronExpression, baseTime, nextTime);
            return nextTime;

        } catch (Exception e) {
            log.error("解析 CRON 表达式失败: {}", cronExpression, e);
            return null;
        }
    }

    @Override
    public boolean validateExpression(String expression) {
        if (expression == null || expression.isEmpty()) {
            return false;
        }

        try {
            CronExpression.parse(expression);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getDescription(String expression) {
        if (!validateExpression(expression)) {
            return "无效的 CRON 表达式";
        }
        return "CRON: " + expression;
    }
}

