package com.simple.pulsejob.client;

import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.metadata.MessageWrapper;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Job 执行上下文.
 *
 * <p>包含任务执行所需的全部信息：</p>
 * <ul>
 *   <li>通信通道</li>
 *   <li>请求信息</li>
 *   <li>处理器名称</li>
 *   <li>执行参数与结果</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class JobContext {

    private final JChannel channel;
    private final JRequest request;
    private final String handlerName;

    @Setter
    private Object[] args;

    @Setter
    private Object result;

    @Setter
    private Throwable cause;

    // ==================== 静态工厂方法 ====================

    /**
     * 从请求消息创建上下文
     *
     * @param channel 通信通道
     * @param request 请求
     * @param message 消息体
     * @return JobContext
     */
    public static JobContext of(JChannel channel, JRequest request, MessageWrapper message) {
        return new JobContext(channel, request, message.getHandlerName());
    }

    /**
     * 从请求创建上下文（handlerName 从 request 中提取）
     */
    public static JobContext of(JChannel channel, JRequest request) {
        MessageWrapper message = request.getMessage();
        return of(channel, request, message);
    }

    // ==================== 便捷方法 ====================

    /**
     * 获取调用ID
     */
    public long invokeId() {
        return request.invokeId();
    }

    /**
     * 是否执行成功
     */
    public boolean isSuccess() {
        return cause == null;
    }

    /**
     * 是否有执行结果
     */
    public boolean hasResult() {
        return result != null;
    }

    @Override
    public String toString() {
        return "JobContext{" +
                "invokeId=" + invokeId() +
                ", handlerName='" + handlerName + '\'' +
                ", success=" + isSuccess() +
                '}';
    }
}
