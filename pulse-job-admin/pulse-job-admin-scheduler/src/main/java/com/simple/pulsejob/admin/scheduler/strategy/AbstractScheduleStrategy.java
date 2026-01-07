package com.simple.pulsejob.admin.scheduler.strategy;

import com.simple.pulsejob.admin.common.model.entity.JobInfo;
import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * 调度策略抽象基类
 *
 * @author pulse
 */
@Slf4j
public abstract class AbstractScheduleStrategy implements ScheduleStrategy {

    @Override
    public LocalDateTime calculateNextExecuteTime(JobInfo jobInfo) {
        if (jobInfo == null) {
            return null;
        }

        // 构建简化的上下文
        ScheduleContext context = new ScheduleContext();
        context.setScheduleType(ScheduleStrategy.Type.from(jobInfo.getScheduleType()));
        context.setScheduleExpression(jobInfo.getScheduleRate());

        return calculateNextExecuteTime(context, jobInfo.getLastExecuteTime());
    }

    /**
     * 解析表达式为秒数
     *
     * @param expression 表达式（支持数字或带单位：10s, 5m, 1h）
     * @return 秒数
     */
    protected long parseToSeconds(String expression) {
        if (expression == null || expression.isEmpty()) {
            throw new IllegalArgumentException("Expression cannot be empty");
        }

        expression = expression.trim().toLowerCase();

        // 纯数字，默认秒
        if (expression.matches("\\d+")) {
            return Long.parseLong(expression);
        }

        // 带单位
        if (expression.endsWith("s")) {
            return Long.parseLong(expression.substring(0, expression.length() - 1));
        } else if (expression.endsWith("m")) {
            return Long.parseLong(expression.substring(0, expression.length() - 1)) * 60;
        } else if (expression.endsWith("h")) {
            return Long.parseLong(expression.substring(0, expression.length() - 1)) * 3600;
        } else if (expression.endsWith("d")) {
            return Long.parseLong(expression.substring(0, expression.length() - 1)) * 86400;
        }

        throw new IllegalArgumentException("Invalid expression format: " + expression);
    }

    /**
     * 格式化秒数为可读描述
     */
    protected String formatSeconds(long seconds) {
        if (seconds < 60) {
            return seconds + "秒";
        } else if (seconds < 3600) {
            return (seconds / 60) + "分钟";
        } else if (seconds < 86400) {
            return (seconds / 3600) + "小时";
        } else {
            return (seconds / 86400) + "天";
        }
    }
}

