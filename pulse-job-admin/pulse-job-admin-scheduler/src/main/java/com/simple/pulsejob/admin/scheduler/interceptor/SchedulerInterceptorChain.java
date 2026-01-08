package com.simple.pulsejob.admin.scheduler.interceptor;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import com.simple.pulsejob.transport.JRequest;
import com.simple.pulsejob.transport.JResponse;
import com.simple.pulsejob.transport.channel.JChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SchedulerInterceptorChain {

    private final List<SchedulerInterceptor> interceptors;

    public SchedulerInterceptorChain(ObjectProvider<List<SchedulerInterceptor>> provider) {
        List<SchedulerInterceptor> list = provider.getIfAvailable(Collections::emptyList);
        this.interceptors = list.stream()
            .sorted(AnnotationAwareOrderComparator.INSTANCE)
            .toList();
    }

    /* ================== 正常流程 ================== */

    public void beforeSchedule(ScheduleContext context, JChannel channel) {
        forEach(i -> i.beforeSchedule(context, channel));
    }

    public void beforeTransport(ScheduleContext context, JChannel channel) {
        forEach(i -> i.beforeTransport(context, channel));
    }

    public void afterTransport(ScheduleContext context, JChannel channel, JRequest request) {
        forEach(i -> i.afterTransport(context, channel, request));
    }

    public void afterSchedule(ScheduleContext context, JChannel channel, JResponse response) {
        forEach(i -> i.afterSchedule(context, channel, response));
    }

    /* ================== 异常流程 ================== */

    public void onTransportFailure(ScheduleContext context, JChannel channel, JRequest request, Throwable throwable) {
        forEach(i -> i.onTransportFailure(context, channel, request, throwable));
    }

    public void onScheduleFailure(ScheduleContext context, JChannel channel, JRequest request) {
        forEach(i -> i.onScheduleFailure(context, channel, request));
    }

    /* ================== 内部工具 ================== */

    private void forEach(Consumer<SchedulerInterceptor> consumer) {
        for (SchedulerInterceptor interceptor : interceptors) {
            try {
                consumer.accept(interceptor);
            } catch (Throwable ex) {
                // ⚠️ 拦截器异常不能影响主流程
                log.error("SchedulerInterceptor error: {}", interceptor.getClass().getName(), ex);
            }
        }
    }
}
