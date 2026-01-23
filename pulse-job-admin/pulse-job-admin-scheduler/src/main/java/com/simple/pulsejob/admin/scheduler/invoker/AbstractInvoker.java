package com.simple.pulsejob.admin.scheduler.invoker;

import com.simple.pulsejob.admin.common.model.enums.InvokeStrategyEnum;
import com.simple.pulsejob.admin.scheduler.ScheduleConfig;
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

    protected Object doInvoke(ScheduleConfig config) throws Throwable {
        Objects.requireNonNull(config, "ScheduleConfig is required");

        ScheduleContext context = ScheduleContext.of(config);

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
                if (throwable == null) {
                    byte status = response.status();
                    ResultWrapper wrapper = response.result();
                    if (status == Status.OK.value()) {
                        schedulerInterceptorChain.afterSchedule(context, response);
                    } else {
                        // 任务执行失败，向上游回传异常
                        Object result = wrapper != null ? wrapper.getResult() : null;
                        Throwable cause = (result instanceof Throwable)
                            ? (Throwable) result
                            : new RuntimeException("任务执行失败, status=" + Status.parse(status) + ", result=" + result);
                        schedulerInterceptorChain.onScheduleFailure(context, response, cause);
                    }
                } else {
                    // 网络异常等情况
                    schedulerInterceptorChain.onScheduleFailure(context, null, throwable);
                }
            });

            // 异步模式，立即返回
            return null;

        } catch (Throwable e) {
            context.markFailed(e);
            log.error("调度失败: jobId={}", context.getJobId(), e);
            throw e;
        }
    }
}
