package com.simple.pulsejob.client;

import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.metadata.MessageWrapper;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Job 执行上下文.
 *
 * <p>包含任务执行所需的全部信息：</p>
 * <ul>
 *   <li>通信通道</li>
 *   <li>请求信息</li>
 *   <li>处理器名称</li>
 *   <li>执行参数与结果</li>
 *   <li>扩展属性（供拦截器使用）</li>
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

    /** 扩展属性（供拦截器等组件使用） */
    private Map<String, Object> attributes;

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
    public long instanceId() {
        return request.instanceId();
    }

    /**
     * 获取任务ID
     */
    public Integer getJobId() {
        MessageWrapper message = request.getMessage();
        return message != null ? message.getJobId() : null;
    }

    /**
     * 获取执行超时时间（秒）
     *
     * @return 超时时间，0 表示不限制
     */
    public int getTimeoutSeconds() {
        MessageWrapper message = request.getMessage();
        return message != null ? message.getTimeoutSeconds() : 0;
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

    // ==================== 扩展属性 ====================

    /**
     * 设置扩展属性
     */
    public void setAttribute(String key, Object value) {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        attributes.put(key, value);
    }

    /**
     * 获取扩展属性
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key) {
        if (attributes == null) {
            return null;
        }
        return (T) attributes.get(key);
    }

    /**
     * 获取扩展属性（带默认值）
     */
    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String key, T defaultValue) {
        if (attributes == null) {
            return defaultValue;
        }
        T value = (T) attributes.get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 移除扩展属性
     */
    public void removeAttribute(String key) {
        if (attributes != null) {
            attributes.remove(key);
        }
    }

    /**
     * 清空所有扩展属性
     */
    public void clearAttributes() {
        if (attributes != null) {
            attributes.clear();
        }
    }

    @Override
    public String toString() {
        return "JobContext{" +
                "instanceId=" + instanceId() +
                ", handlerName='" + handlerName + '\'' +
                ", success=" + isSuccess() +
                '}';
    }
}
