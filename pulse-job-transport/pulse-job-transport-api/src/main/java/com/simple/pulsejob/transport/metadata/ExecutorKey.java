package com.simple.pulsejob.transport.metadata;


import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class ExecutorKey implements Serializable {

    @Serial
    private static final long serialVersionUID = 1009813828866652853L;

    private transient String executorKeyCache;

    private String executorName;

    /**
     * 执行器业务地址（IP:Port），由执行器配置并上报
     * 用于标识执行器的真实服务地址，而非 Netty 连接的临时端口
     */
    private String executorAddress;

    public ExecutorKey(String executorName) {
        this.executorName = executorName;
    }

    public ExecutorKey(String executorName, String executorAddress) {
        this.executorName = executorName;
        this.executorAddress = executorAddress;
    }

    public static ExecutorKey of(String executorName) {
        return new ExecutorKey(executorName);
    }

    public static ExecutorKey of(String executorName, String executorAddress) {
        return new ExecutorKey(executorName, executorAddress);
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
