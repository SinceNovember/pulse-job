package com.simple.pulsejob.admin.business.interceptor;

import com.simple.pulsejob.admin.business.service.IJobInstanceService;
import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import com.simple.pulsejob.admin.scheduler.future.DefaultInvokeFuture;
import com.simple.pulsejob.admin.scheduler.interceptor.SchedulerInterceptor;
import com.simple.pulsejob.transport.channel.JChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 任务实例结果处理拦截器.
 *
 * <p>在任务执行完成后，更新 JobInstance 的最终状态（成功/失败）。</p>
 * <p>通过注册 Future 回调来异步处理执行结果。</p>
 *
 * <p>执行顺序：在 JobInstanceLifecycleInterceptor 之后</p>
 */
@Slf4j
@Component
@Order(200)
@RequiredArgsConstructor
public class JobInstanceResultInterceptor implements SchedulerInterceptor {

    private final IJobInstanceService jobInstanceService;

    @Override
    public void afterTransport(ScheduleContext context) {
        Long instanceId = context.getInstanceId();
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

    // 其他生命周期方法使用默认空实现

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
