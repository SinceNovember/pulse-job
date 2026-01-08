package com.simple.pulsejob.admin.scheduler.invoker;

import com.simple.plusejob.serialization.SerializerType;
import com.simple.pulsejob.admin.common.model.entity.JobInstance;
import com.simple.pulsejob.admin.scheduler.ScheduleConfig;
import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import com.simple.pulsejob.admin.scheduler.cluster.ClusterInvoker;
import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.metadata.MessageWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 抽象调用器.
 *
 * <p>所有调度都必须创建 JobInstance，使用数据库生成的 instanceId 作为请求标识</p>
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractInvoker implements Invoker {

    private final ClusterInvoker clusterInvoker;

    protected Object doInvoke(ScheduleConfig config) throws Throwable {
        Objects.requireNonNull(config, "ScheduleConfig is required");

        ScheduleContext context = ScheduleContext.of(config);
        clusterInvoker.invoke(context);
        return context.getResult();
//        // 5. 执行过滤器链
//        try {
//            clusterInvoker.invoke(context);
//
//            // 成功时更新状态
//            if (context.isSuccess()) {
//                jobInstanceManager.markSuccess(instanceId, LocalDateTime.now());
//            }
//
//            return context.getResult();
//        } catch (Throwable e) {
//            // 失败时更新实例状态
//            jobInstanceManager.markFailed(instanceId, LocalDateTime.now(), e.getMessage());
//            throw e;
//        }
    }

    /**
     * 创建请求
     */
    private JRequest createRequest(long instanceId, String handlerName, String args) {
        MessageWrapper message = new MessageWrapper(handlerName, args);

        JRequest request = new JRequest(instanceId);
        request.setMessage(message);
        request.getPayload().setSerializerCode(SerializerType.JAVA.value());
        return request;
    }
}
