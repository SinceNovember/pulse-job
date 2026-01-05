package com.simple.pulsejob.client.processor;

import com.simple.plusejob.serialization.Serializer;
import com.simple.plusejob.serialization.SerializerType;
import com.simple.pulsejob.client.JobContext;
import com.simple.pulsejob.client.autoconfigure.PulseJobClientProperties;
import com.simple.pulsejob.client.invoker.Invoker;
import com.simple.pulsejob.client.log.JobLogSender;
import com.simple.pulsejob.client.processor.task.MessageTask;
import com.simple.pulsejob.client.registry.JobBeanDefinition;
import com.simple.pulsejob.client.registry.JobBeanDefinitionLookupService;
import com.simple.pulsejob.client.registry.JobBeanDefinitionRegistry;
import com.simple.pulsejob.client.serialization.SerializerHolder;
import com.simple.pulsejob.common.concurrent.executor.CloseableExecutor;
import com.simple.pulsejob.common.util.StackTraceUtil;
import com.simple.pulsejob.common.util.ThrowUtil;
import com.simple.pulsejob.common.util.internal.logging.InternalLogger;
import com.simple.pulsejob.common.util.internal.logging.InternalLoggerFactory;
import com.simple.pulsejob.transport.JProtocolHeader;
import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.Status;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.channel.JFutureListener;
import com.simple.pulsejob.transport.metadata.ExecutorKey;
import com.simple.pulsejob.transport.metadata.ResultWrapper;
import com.simple.pulsejob.transport.payload.JRequestPayload;
import com.simple.pulsejob.transport.payload.JResponsePayload;
import com.simple.pulsejob.transport.payload.PayloadSerializer;
import com.simple.pulsejob.transport.processor.ConnectorProcessor;
import lombok.RequiredArgsConstructor;

/**
 * 客户端消息处理器.
 *
 * <p>负责处理与 Admin 服务端的通信：</p>
 * <ul>
 *   <li>连接建立时自动注册执行器</li>
 *   <li>接收并处理任务执行请求</li>
 *   <li>发送任务执行结果/异常</li>
 * </ul>
 */
@RequiredArgsConstructor
public class DefaultClientProcessor implements ConnectorProcessor, JobBeanDefinitionLookupService {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultClientProcessor.class);

    private final CloseableExecutor executor;
    private final SerializerHolder serializerHolder;
    private final JobBeanDefinitionRegistry jobBeanDefinitionRegistry;
    private final Invoker invoker;
    private final PulseJobClientProperties clientProperties;
    private final JobLogSender jobLogSender;

    /** 当前活跃的 Channel */
    private volatile JChannel activeChannel;

    // ==================== ConnectorProcessor 实现 ====================

    @Override
    public Serializer serializer(Byte code) {
        return serializerHolder.getByCode(code);
    }

    @Override
    public void handleActive(JChannel channel) {
        logger.info("Channel active: {}", channel.remoteAddress());
        this.activeChannel = channel;
        // 注册执行器
        sendRegisterExecutorRequest(channel);
        // 绑定日志发送通道
        jobLogSender.bindChannel(channel);
    }

    @Override
    public void handleInactive(JChannel channel) {
        logger.warn("Channel inactive: {}", channel.remoteAddress());
        if (this.activeChannel == channel) {
            this.activeChannel = null;
        }
    }

    @Override
    public void handleRequest(JChannel channel, JRequestPayload requestPayload) {
        MessageTask task = new MessageTask(this, channel, new JRequest(requestPayload));

        if (executor != null) {
            executor.execute(task);
        } else {
            // 没有线程池时，使用 Channel 的任务队列
            channel.addTask(task);
        }
    }

    @Override
    public void handleException(JChannel channel, JRequestPayload request, Status status, Throwable cause) {
        logger.error("Exception while handling request from {}: {}",
                channel.remoteAddress(), cause.getMessage());
        doHandleException(channel, request.invokeId(), request.serializerCode(), status, cause, false);
    }

    @Override
    public void shutdown() {
        logger.info("DefaultClientProcessor shutting down...");
        if (executor != null) {
            executor.shutdown();
        }
    }

    // ==================== JobBeanDefinitionLookupService 实现 ====================

    @Override
    public JobBeanDefinition getJobBeanDefinition(String jobBeanDefinitionName) {
        return jobBeanDefinitionRegistry.getJobBeanDefinition(jobBeanDefinitionName);
    }

    // ==================== 业务方法 ====================

    /**
     * 调用 Job 处理器
     */
    public Object invoke(JobContext jobContext) {
        return invoker.invoke(jobContext);
    }

    /**
     * 处理请求异常（由 MessageTask 调用）
     */
    public void handleRequestException(JChannel channel, JRequest request, Status status, Throwable cause) {
        logger.error("Exception while processing request: {}, {}",
                channel.remoteAddress(), StackTraceUtil.stackTrace(cause));
        doHandleException(channel, request.invokeId(), request.serializerCode(), status, cause, false);
    }

    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        JChannel channel = activeChannel;
        return channel != null && channel.isActive();
    }

    // ==================== 私有方法 ====================

    /**
     * 发送注册执行器请求
     */
    private void sendRegisterExecutorRequest(JChannel channel) {
        ExecutorKey executorKey = new ExecutorKey(clientProperties.getExecutorName());

        JRequestPayload payload = PayloadSerializer.request()
                .channel(channel)
                .type(SerializerType.JAVA)
                .message(executorKey)
                .messageCode(JProtocolHeader.REGISTER_EXECUTOR)
                .build();

        channel.write(payload, LoggingFutureListener.REGISTER);
        logger.info("Sent executor register request: {}", executorKey.getExecutorName());
    }

    /**
     * 发送错误响应
     */
    private void doHandleException(JChannel channel, long invokeId, byte serializerCode,
                                   Status status, Throwable cause, boolean closeChannel) {
        ResultWrapper result = new ResultWrapper();
        result.setError(ThrowUtil.cutCause(cause));

        SerializerType type = SerializerType.parse(serializerCode);
        JResponsePayload response = PayloadSerializer.response()
                .invokeId(invokeId)
                .channel(channel)
                .type(type != null ? type : SerializerType.JAVA)
                .message(result)
                .messageCode(JProtocolHeader.JOB_RESULT)
                .build();
        response.status(status.value());

        if (closeChannel) {
            channel.write(response, JChannel.CLOSE);
        } else {
            channel.write(response, LoggingFutureListener.ERROR_RESPONSE);
        }
    }

    // ==================== 静态内部类 ====================

    /**
     * 日志记录的 FutureListener（复用，避免重复创建匿名类）
     */
    private enum LoggingFutureListener implements JFutureListener<JChannel> {
        REGISTER {
            @Override
            public void operationSuccess(JChannel channel) {
                logger.debug("Register request sent successfully: {}", channel);
            }

            @Override
            public void operationFailure(JChannel channel, Throwable cause) {
                logger.error("Register request sent failed: {}", channel, cause);
            }
        },

        ERROR_RESPONSE {
            @Override
            public void operationSuccess(JChannel channel) {
                logger.debug("Error response sent: {}", channel);
            }

            @Override
            public void operationFailure(JChannel channel, Throwable cause) {
                logger.warn("Error response sent failed: {}, {}", channel, cause.getMessage());
            }
        }
    }
}
