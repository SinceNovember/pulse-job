package com.simple.pulsejob.transport.metadata;


import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class ExecutorKey implements Serializable {

    @Serial
    private static final long serialVersionUID = 1009813828866652852L;

    private transient String executorKeyCache;

    private String executorName;

    public ExecutorKey(String executorName) {
        this.executorName = executorName;
    }

    public String exeuctorKeyString() {
        if (executorKeyCache != null) {
            return executorKeyCache;
        }

        StringBuilder buf = new StringBuilder();
        buf.append(executorName);

        executorKeyCache = buf.toString();

        return executorKeyCache;
    }

}
