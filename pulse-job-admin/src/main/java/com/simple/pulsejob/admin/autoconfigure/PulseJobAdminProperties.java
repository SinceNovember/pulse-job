package com.simple.pulsejob.admin.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = PulseJobAdminProperties.PULSE_JOB_PREFIX)
public class PulseJobAdminProperties {

    public static final String PULSE_JOB_PREFIX = "pulse.job.admin";

    private String host;

    private Integer port = 10410;
}
