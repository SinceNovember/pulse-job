package com.simple.pulsejob.admin.scheduler.strategy;

import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 固定频率调度策略
 * <p>
 * 任务按照固定的时间间隔执行，不管上一次任务是否执行完成
 * </p>
 *
 * @author pulse
 */
@Slf4j
@Component
public class FixedRateScheduleStrategy extends AbstractScheduleStrategy {

    @Override
    public Type getType() {
        return Type.FIXED_RATE;
    }

    @Override
    public LocalDateTime calculateNextExecuteTime(ScheduleContext context, LocalDateTime lastExecuteTime) {
        String expression = context.getScheduleExpression();

        if (expression == null || expression.isEmpty()) {
            log.warn("固定频率表达式为空，无法计算下次执行时间");
            return null;
        }

        try {
            long intervalSeconds = parseToSeconds(expression);

            // 固定频率：基于上次执行时间 + 间隔
            // 如果上次执行时间为空，基于当前时间
            LocalDateTime baseTime = lastExecuteTime != null ? lastExecuteTime : LocalDateTime.now();
            LocalDateTime nextTime = baseTime.plusSeconds(intervalSeconds);

            // 如果计算出的时间已过期，以当前时间为基准重新计算
            if (nextTime.isBefore(LocalDateTime.now())) {
                nextTime = LocalDateTime.now().plusSeconds(intervalSeconds);
            }

            log.debug("固定频率[{}秒] 基于 {} 计算下次执行时间: {}", intervalSeconds, baseTime, nextTime);
            return nextTime;

        } catch (Exception e) {
            log.error("解析固定频率表达式失败: {}", expression, e);
            return null;
        }
    }

    @Override
    public boolean validateExpression(String expression) {
        if (expression == null || expression.isEmpty()) {
            return false;
        }

        try {
            long seconds = parseToSeconds(expression);
            return seconds > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getDescription(String expression) {
        if (!validateExpression(expression)) {
            return "无效的固定频率表达式";
        }

        try {
            long seconds = parseToSeconds(expression);
            return "每 " + formatSeconds(seconds) + " 执行一次";
        } catch (Exception e) {
            return "无效的固定频率表达式";
        }
    }
}

