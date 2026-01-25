package com.simple.pulsejob.admin.business.interceptor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simple.pulsejob.admin.business.service.IJobInstanceService;
import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import com.simple.pulsejob.admin.scheduler.future.DefaultInvokeFuture;
import com.simple.pulsejob.admin.scheduler.interceptor.SchedulerInterceptor;
import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.JResponse;
import com.simple.pulsejob.transport.channel.JChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;

/**
 * 任务实例结果处理拦截器.
 *
 * <p>在任务执行完成后，更新 JobInstance 的最终状态（成功/失败）。</p>
 * <p>通过注册 Future 回调来异步处理执行结果。</p>
 *
 * <p>执行顺序：在 JobInstanceLifecycleInterceptor 之后</p>
 *
 * <p>注意：广播/分片模式下会调用多次，通过 request.instanceId() 区分不同实例</p>
 */
@Slf4j
@Component
@Order(200)
@RequiredArgsConstructor
public class JobInstanceResultInterceptor implements SchedulerInterceptor {

    private final IJobInstanceService jobInstanceService;
    private final ObjectMapper objectMapper;

    @Override
    public void afterTransport(ScheduleContext context, JRequest request) {
        Long instanceId = request.instanceId();
        JChannel channel = context.getChannel();
        
        if (instanceId == null) {
            return;
        }

        // ✅ 关键：获取 Future 并注册回调，在任务执行完成后更新状态
        DefaultInvokeFuture future = getFuture(instanceId, channel);
        if (future != null) {
            future.whenComplete((response, throwable) -> {
                try {
                    if (throwable == null) {
                        // 执行成功
                        jobInstanceService.markSuccess(instanceId, LocalDateTime.now());
                        log.info("任务执行成功，已更新实例状态: instanceId={}", instanceId);
                    } else {
                        // 执行失败
                        String errorMsg = throwable.getMessage();
                        if (errorMsg != null && errorMsg.length() > 500) {
                            errorMsg = errorMsg.substring(0, 500);
                        }
                        jobInstanceService.markFailed(instanceId, LocalDateTime.now(), errorMsg);
                        log.warn("任务执行失败，已更新实例状态: instanceId={}, error={}",
                                instanceId, throwable.getMessage());
                    }
                } catch (Exception e) {
                    log.error("更新任务实例状态失败: instanceId={}", instanceId, e);
                }
            });
        }
    }

    // ==================== afterSchedule & onScheduleFailure ====================

    /**
     * 调度完成（任务执行成功）
     * <p>当任务从执行器返回成功结果时调用</p>
     */
    @Override
    public void afterSchedule(ScheduleContext context, JResponse response) {
        Long instanceId = context.getInstanceId();
        if (instanceId == null) {
            log.warn("afterSchedule: instanceId is null, skip");
            return;
        }

        try {
            LocalDateTime endTime = LocalDateTime.now();
            
            // 提取执行结果
            String resultJson = extractResult(response);
            
            // 更新执行器地址
            updateExecutorAddress(context);
            
            // 保存成功状态和结果到数据库
            jobInstanceService.markSuccessWithResult(instanceId, endTime, resultJson);
            
            // 同步更新 context 状态
            context.markSuccess(response.result());
            context.setResponse(response);
            
            log.info("调度成功: instanceId={}, jobId={}, handler={}", 
                    instanceId, context.getJobId(), context.getJobHandler());
            
        } catch (Exception e) {
            log.error("保存调度成功状态失败: instanceId={}", instanceId, e);
        }
    }

    /**
     * 调度失败（任务执行失败）
     * <p>当任务执行失败或发生异常时调用</p>
     */
    @Override
    public void onScheduleFailure(ScheduleContext context, JResponse response, Throwable throwable) {
        Long instanceId = context.getInstanceId();
        if (instanceId == null) {
            log.warn("onScheduleFailure: instanceId is null, skip");
            return;
        }

        try {
            LocalDateTime endTime = LocalDateTime.now();
            
            // 构建错误信息
            String errorMsg = buildErrorMessage(response, throwable);
            String errorDetail = buildErrorDetail(throwable);
            
            // 更新执行器地址
            updateExecutorAddress(context);
            
            // 保存失败状态和错误信息到数据库
            jobInstanceService.markFailedWithDetail(instanceId, endTime, errorMsg, errorDetail);
            
            // 同步更新 context 状态
            context.markFailed(throwable);
            if (response != null) {
                context.setResponse(response);
            }
            
            log.error("调度失败: instanceId={}, jobId={}, handler={}, error={}", 
                    instanceId, context.getJobId(), context.getJobHandler(), errorMsg);
            
        } catch (Exception e) {
            log.error("保存调度失败状态失败: instanceId={}", instanceId, e);
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 从响应中提取结果并转为 JSON 字符串
     */
    private String extractResult(JResponse response) {
        if (response == null || response.result() == null) {
            return null;
        }
        
        Object result = response.result();
        
        // 如果已经是字符串，直接返回
        if (result instanceof String) {
            return (String) result;
        }
        
        // 尝试序列化为 JSON
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            log.warn("序列化执行结果失败，使用 toString: {}", e.getMessage());
            return result.toString();
        }
    }

    /**
     * 构建错误信息
     */
    private String buildErrorMessage(JResponse response, Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        
        // 从响应中获取错误信息
        if (response != null && response.result() != null) {
            sb.append(response.result().toString());
        }
        
        // 从异常中获取错误信息
        if (throwable != null) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append(throwable.getClass().getSimpleName())
              .append(": ")
              .append(throwable.getMessage());
        }
        
        return sb.length() > 0 ? sb.toString() : "Unknown error";
    }

    /**
     * 构建错误详情（堆栈信息）
     */
    private String buildErrorDetail(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String stackTrace = sw.toString();
        
        // 限制堆栈长度
        if (stackTrace.length() > 4000) {
            stackTrace = stackTrace.substring(0, 4000) + "\n... (truncated)";
        }
        
        return stackTrace;
    }

    /**
     * 更新执行器地址
     */
    private void updateExecutorAddress(ScheduleContext context) {
        Long instanceId = context.getInstanceId();
        JChannel channel = context.getChannel();
        
        if (instanceId != null && channel != null) {
            String address = channel.remoteAddress() != null 
                    ? channel.remoteAddress().toString() 
                    : "unknown";
            jobInstanceService.updateExecutorAddress(instanceId, address);
        }
    }

    /**
     * 根据 instanceId 和 channel 获取对应的 Future
     */
    private DefaultInvokeFuture getFuture(Long instanceId, JChannel channel) {
        // 先尝试从单播 Future 中获取
        DefaultInvokeFuture future = DefaultInvokeFuture.getFuture(instanceId);
        if (future != null) {
            return future;
        }

        // 再尝试从广播 Future 中获取
        if (channel != null) {
            return DefaultInvokeFuture.getBroadcastFuture(channel.id(), instanceId);
        }

        return null;
    }
}
