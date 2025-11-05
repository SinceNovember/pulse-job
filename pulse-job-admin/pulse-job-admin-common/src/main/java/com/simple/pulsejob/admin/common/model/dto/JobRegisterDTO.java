package com.simple.pulsejob.admin.common.model.dto;

import com.simple.pulsejob.admin.common.model.enums.ScheduleTypeEnum;
import lombok.Data;

@Data
public class JobRegisterDTO {

    private String jobHandler;

    private String scheduleRate;

    private ScheduleTypeEnum scheduleType;

}
