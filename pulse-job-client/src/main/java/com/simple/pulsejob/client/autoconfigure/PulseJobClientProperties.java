package com.simple.pulsejob.client.autoconfigure;

import com.simple.pulsejob.common.JConstants;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = PulseJobClientProperties.PULSE_JOB_PREFIX)
public class PulseJobClientProperties {

    public static final String PULSE_JOB_PREFIX = "pulse.job";

    private String executor;

    private int corePoolSize = JConstants.DEFAULT_CORE_POOL_SIZE;

    private int workQueue = 1000;

    private int maxPoolSize = JConstants.DEFAULT_MAX_POOL_SIZE;

    private String executorName;

    private String serializerType;

    private PulseJobAdmin admin;

    @Data
    public static class PulseJobAdmin {

        private String host;

        private Integer port = 10416;

    }
}
