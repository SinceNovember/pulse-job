package com.simple.pulsejob.admin.scheduler.invoker;

import com.simple.pulsejob.admin.common.model.enums.InvokeStrategyEnum;
import com.simple.pulsejob.admin.scheduler.JobInstanceStatusManager;
import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import com.simple.pulsejob.admin.scheduler.cluster.ClusterInvoker;
import com.simple.pulsejob.admin.scheduler.factory.ClusterInvokerFactory;
import com.simple.pulsejob.admin.scheduler.future.InvokeFuture;
import com.simple.pulsejob.admin.scheduler.interceptor.SchedulerInterceptorChain;
import com.simple.pulsejob.transport.Status;
import com.simple.pulsejob.transport.metadata.ResultWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.concurrent.TimeoutException;

/**
 * 抽象调用器.
 *
 * <p>完整调度流程：</p>
 * <ol>
 *   <li>beforeSchedule - 拦截器前置处理（查询 JobInfo、设置 ExecutorKey）</li>
 *   <li>根据策略选择 ClusterInvoker</li>
 *   <li>ClusterInvoker.invoke() - 包含 beforeTransport、网络传输、afterTransport</li>
 *   <li>注册异步回调处理执行结果</li>
 * </ol>
 *
 * <p>注意：分布式定时任务统一采用异步模式，不支持同步阻塞等待。</p>
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractInvoker implements Invoker {

    private final ClusterInvokerFactory clusterInvokerFactory;
    private final SchedulerInterceptorChain schedulerInterceptorChain;
    private final JobInstanceStatusManager statusManager;

    protected Object doInvoke(ScheduleContext context) throws Throwable {
        Objects.requireNonNull(context, "ScheduleContext is required");

        try {
            // 1. 拦截器前置处理
            schedulerInterceptorChain.beforeSchedule(context);

            // 2. 根据策略选择 ClusterInvoker
            InvokeStrategyEnum strategy = context.getInvokeStrategy();
            if (strategy == null) {
                strategy = InvokeStrategyEnum.getDefault();
            }
            ClusterInvoker clusterInvoker = clusterInvokerFactory.get(strategy);

            log.debug("使用集群策略: {}, jobId={}", strategy, context.getJobId());

            // 3. 执行调用（异步）
            InvokeFuture future = clusterInvoker.invoke(context);

            // 4. 注册异步回调
            future.whenComplete((response, throwable) -> {
                Long instanceId = context.getInstanceId();
                
                if (throwable == null) {
                    byte status = response.status();
                    ResultWrapper wrapper = response.result();
                    
                    if (status == Status.OK.value()) {
                        // ✅ 核心逻辑：更新状态为成功 + 保存结果
                        statusManager.markSuccess(instanceId, extractResult(wrapper));
                        // 扩展逻辑：调用拦截器
                        schedulerInterceptorChain.afterSchedule(context, response);
                    } else {
                        // ✅ 核心逻辑：更新状态为失败 + 保存错误信息
                        Object result = wrapper != null ? wrapper.getResult() : null;
                        Throwable cause = (result instanceof Throwable)
                            ? (Throwable) result
                            : new RuntimeException("任务执行失败, status=" + Status.parse(status) + ", result=" + result);
                        
                        // 判断是否超时（客户端返回的超时异常）
                        if (status == Status.CLIENT_TIMEOUT.value()) {
                            statusManager.markTimeout(instanceId, cause.getMessage());
                        } else {
                            statusManager.markFailed(instanceId, cause);
                        }
                        // 扩展逻辑：调用拦截器
                        schedulerInterceptorChain.onScheduleFailure(context, response, cause);
                    }
                } else {
                    // ✅ 核心逻辑：网络异常或超时
                    if (isTimeoutException(throwable)) {
                        // Admin 侧超时（客户端未在规定时间内返回）
                        statusManager.markTimeout(instanceId, throwable.getMessage());
                    } else {
                        statusManager.markFailed(instanceId, throwable);
                    }
                    // 扩展逻辑：调用拦截器
                    schedulerInterceptorChain.onScheduleFailure(context, null, throwable);
                }
            });

            // 异步模式，立即返回
            return null;

        } catch (Throwable e) {
            log.error("调度失败: jobId={}", context.getJobId(), e);
            throw e;
        }
    }

    /**
     * 提取执行结果
     */
    private String extractResult(ResultWrapper wrapper) {
        if (wrapper == null || wrapper.getResult() == null) {
            return null;
        }
        Object result = wrapper.getResult();
        // 客户端已经转为 JSON 字符串了
        return result instanceof String ? (String) result : result.toString();
    }

    /**
     * 判断是否为超时异常
     */
    private boolean isTimeoutException(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        // 检查异常类型
        if (throwable instanceof TimeoutException) {
            return true;
        }
        // 检查异常消息（客户端返回的超时信息）
        String message = throwable.getMessage();
        return message != null && message.toLowerCase().contains("timeout");
    }
}
