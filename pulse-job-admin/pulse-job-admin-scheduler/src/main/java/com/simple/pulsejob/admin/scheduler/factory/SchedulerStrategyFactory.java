package com.simple.pulsejob.admin.scheduler.factory;

import java.util.List;
import java.util.function.Function;
import com.simple.pulsejob.admin.scheduler.strategy.ScheduleStrategy;
import org.springframework.stereotype.Component;

@Component
public class SchedulerStrategyFactory extends EnumBeanFactory<ScheduleStrategy.Type, ScheduleStrategy> {

    protected SchedulerStrategyFactory(List<ScheduleStrategy> beans) {
        super(beans, ScheduleStrategy::getType, "Duplicate schedule strategy detected: ");
    }
}
