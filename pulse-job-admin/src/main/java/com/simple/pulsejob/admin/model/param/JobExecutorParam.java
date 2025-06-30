package com.simple.pulsejob.admin.model.param;

import com.simple.pulsejob.admin.model.enums.RegisterTypeEnum;
import lombok.Data;

@Data
public class JobExecutorParam {

    private String executorName;

    private String executorDesc;

    private RegisterTypeEnum registerType;

    private String executorAddress;
} 