package com.simple.pulsejob.admin.scheduler.strategy;

import com.simple.pulsejob.admin.common.model.enums.ScheduleTypeEnum;
import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 固定延迟调度策略
 * <p>
 * 任务在上一次执行完成后，等待固定时间再执行下一次
 * </p>
 *
 * @author pulse
 */
@Slf4j
@Component
public class FixedDelayScheduleStrategy extends AbstractScheduleStrategy {

    @Override
    public ScheduleTypeEnum getType() {
        return ScheduleTypeEnum.FIXED_DELAY;
    }

    @Override
    public LocalDateTime calculateNextExecuteTime(ScheduleContext context, LocalDateTime lastExecuteTime) {
        String expression = context.getScheduleExpression();

        if (expression == null || expression.isEmpty()) {
            log.warn("固定延迟表达式为空，无法计算下次执行时间");
            return null;
        }

        try {
            long delaySeconds = parseToSeconds(expression);

            // 固定延迟：基于当前时间 + 延迟
            // 因为固定延迟是任务完成后再延迟，所以始终基于当前时间
            LocalDateTime nextTime = LocalDateTime.now().plusSeconds(delaySeconds);

            log.debug("固定延迟[{}秒] 计算下次执行时间: {}", delaySeconds, nextTime);
            return nextTime;

        } catch (Exception e) {
            log.error("解析固定延迟表达式失败: {}", expression, e);
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
            return "无效的固定延迟表达式";
        }

        try {
            long seconds = parseToSeconds(expression);
            return "执行完成后延迟 " + formatSeconds(seconds) + " 再执行";
        } catch (Exception e) {
            return "无效的固定延迟表达式";
        }
    }
}

