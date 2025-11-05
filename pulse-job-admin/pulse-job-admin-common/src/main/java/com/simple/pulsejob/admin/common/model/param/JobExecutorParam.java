package com.simple.pulsejob.admin.common.model.param;

import com.simple.pulsejob.admin.common.model.enums.RegisterTypeEnum;
import lombok.Data;

@Data
public class JobExecutorParam {

    private String executorName;

    private String executorDesc;

    private RegisterTypeEnum registerType;

    private String executorAddress;
} 