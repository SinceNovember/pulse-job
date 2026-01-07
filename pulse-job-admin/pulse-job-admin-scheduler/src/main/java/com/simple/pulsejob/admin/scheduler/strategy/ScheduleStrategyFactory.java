package com.simple.pulsejob.admin.scheduler.strategy;

import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 调度策略工厂
 * <p>
 * 根据 {@link ScheduleContext} 中的调度类型获取对应的策略实现
 * </p>
 *
 * @author pulse
 */
@Slf4j
@Component
public class ScheduleStrategyFactory {

    private final Map<ScheduleStrategy.Type, ScheduleStrategy> strategyMap;

    /**
     * 通过 Spring 自动注入所有策略实现
     */
    public ScheduleStrategyFactory(List<ScheduleStrategy> strategies) {
        this.strategyMap = new EnumMap<>(ScheduleStrategy.Type.class);

        for (ScheduleStrategy strategy : strategies) {
            strategyMap.put(strategy.getType(), strategy);
            log.info("注册调度策略: {} -> {}", strategy.getType(), strategy.getClass().getSimpleName());
        }

        // 验证所有类型都有对应的策略
        for (ScheduleStrategy.Type type : ScheduleStrategy.Type.values()) {
            if (!strategyMap.containsKey(type)) {
                log.warn("调度类型 {} 未找到对应的策略实现", type);
            }
        }
    }

    /**
     * 根据调度上下文获取策略
     *
     * @param context 调度上下文
     * @return 调度策略
     * @throws IllegalArgumentException 如果找不到对应的策略
     */
    public ScheduleStrategy getStrategy(ScheduleContext context) {
        if (context == null || context.getScheduleType() == null) {
            throw new IllegalArgumentException("ScheduleContext 或 ScheduleType 不能为空");
        }
        return getStrategy(context.getScheduleType());
    }

    /**
     * 根据调度类型获取策略
     *
     * @param type 调度类型
     * @return 调度策略
     * @throws IllegalArgumentException 如果找不到对应的策略
     */
    public ScheduleStrategy getStrategy(ScheduleStrategy.Type type) {
        ScheduleStrategy strategy = strategyMap.get(type);

        if (strategy == null) {
            throw new IllegalArgumentException("未找到调度类型 " + type + " 对应的策略实现");
        }

        return strategy;
    }

    /**
     * 判断是否支持指定的调度类型
     *
     * @param type 调度类型
     * @return true-支持，false-不支持
     */
    public boolean supports(ScheduleStrategy.Type type) {
        return strategyMap.containsKey(type);
    }

    /**
     * 验证调度表达式
     *
     * @param type       调度类型
     * @param expression 调度表达式
     * @return true-合法，false-不合法
     */
    public boolean validateExpression(ScheduleStrategy.Type type, String expression) {
        ScheduleStrategy strategy = strategyMap.get(type);
        if (strategy == null) {
            return false;
        }
        return strategy.validateExpression(expression);
    }

    /**
     * 获取调度描述
     *
     * @param type       调度类型
     * @param expression 调度表达式
     * @return 描述信息
     */
    public String getDescription(ScheduleStrategy.Type type, String expression) {
        ScheduleStrategy strategy = strategyMap.get(type);
        if (strategy == null) {
            return "未知的调度类型: " + type;
        }
        return strategy.getDescription(expression);
    }
}

