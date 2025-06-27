package com.simple.pulsejob.transport.metadata;


import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JobExecutorWrapper implements Serializable {

    @Serial
    private static final long serialVersionUID = 1009813828866652852L;

    private String executorName;

}
