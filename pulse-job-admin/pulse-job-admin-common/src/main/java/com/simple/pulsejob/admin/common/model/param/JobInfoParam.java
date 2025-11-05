package com.simple.pulsejob.admin.common.model.param;

import com.simple.pulsejob.admin.common.model.enums.ScheduleTypeEnum;
import lombok.Data;

@Data
public class JobInfoParam {

    private String jobHandler;

    private String scheduleRate;

    private ScheduleTypeEnum scheduleType;

    private Integer executorId;

}
