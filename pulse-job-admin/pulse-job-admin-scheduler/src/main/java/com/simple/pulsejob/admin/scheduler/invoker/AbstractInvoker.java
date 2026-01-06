package com.simple.pulsejob.admin.scheduler.invoker;

import com.simple.plusejob.serialization.SerializerType;
import com.simple.pulsejob.admin.common.model.entity.JobInstance;
import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import com.simple.pulsejob.admin.scheduler.cluster.ClusterInvoker;
import com.simple.pulsejob.admin.scheduler.filter.JobFilterChains;
import com.simple.pulsejob.admin.scheduler.instance.JobInstanceManager;
import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.metadata.MessageWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 抽象调用器.
 *
 * <p>所有调度都必须创建 JobInstance，使用数据库生成的 instanceId 作为 invokeId</p>
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractInvoker implements Invoker {

    private final ClusterInvoker clusterInvoker;
    private final JobFilterChains chains;
    private final JobInstanceManager jobInstanceManager;

    /**
     * 执行调度
     *
     * @param executorName 执行器名称
     * @param jobId        任务ID
     * @param executorId   执行器ID
     * @param handlerName  处理器名称
     * @param args         参数
     * @param sync         是否同步
     */
    protected Object doInvoke(String executorName, Long jobId, Long executorId,
                              String handlerName, String args, boolean sync) throws Throwable {
        Objects.requireNonNull(jobInstanceManager, "JobInstanceManager is required");

        // 1. 创建任务实例（必须，instanceId 即 invokeId）
        JobInstance instance = jobInstanceManager.createInstance(jobId, executorId);
        Long instanceId = instance.getId();
        log.info("Created job instance: instanceId={}, jobId={}, executorId={}",
                instanceId, jobId, executorId);

        // 2. 创建请求（使用 instanceId 作为 invokeId）
        JRequest request = createRequest(instanceId, handlerName, args);

        // 3. 创建上下文
        ScheduleContext context = new ScheduleContext(executorName, jobId, executorId, clusterInvoker, sync);
        context.setInstanceId(instanceId);

        // 4. 标记为已发送
        jobInstanceManager.markDispatched(instanceId);

        // 5. 执行过滤器链
        try {
            chains.doFilter(request, context);

            // 成功时更新状态
            if (context.isSuccess()) {
                jobInstanceManager.markSuccess(instanceId, LocalDateTime.now());
            }

            return context.getResult();
        } catch (Throwable e) {
            // 失败时更新实例状态
            jobInstanceManager.markFailed(instanceId, LocalDateTime.now(), e.getMessage());
            throw e;
        }
    }

    /**
     * 创建请求（使用 instanceId 作为 invokeId）
     */
    private JRequest createRequest(long instanceId, String handlerName, String args) {
        MessageWrapper message = new MessageWrapper(handlerName, args);

        JRequest request = new JRequest(instanceId);
        request.setMessage(message);
        request.getPayload().setSerializerCode(SerializerType.JAVA.value());
        return request;
    }
}
