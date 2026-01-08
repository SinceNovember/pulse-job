package com.simple.pulsejob.admin.scheduler.factory;

import com.simple.pulsejob.admin.common.model.enums.ScheduleTypeEnum;
import com.simple.pulsejob.admin.scheduler.strategy.ScheduleStrategy;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SchedulerStrategyFactory extends EnumBeanFactory<ScheduleTypeEnum, ScheduleStrategy> {

    protected SchedulerStrategyFactory(List<ScheduleStrategy> beans) {
        super(beans, ScheduleStrategy::getType, "Duplicate schedule strategy detected: ");
    }
}
