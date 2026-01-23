package com.simple.pulsejob.admin.scheduler.interceptor;

import com.simple.pulsejob.admin.scheduler.ScheduleContext;
import com.simple.pulsejob.transport.JResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

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

    public void beforeSchedule(ScheduleContext context) {
        forEach(i -> i.beforeSchedule(context));
    }

    public void beforeTransport(ScheduleContext context) {
        forEach(i -> i.beforeTransport(context));
    }

    public void afterTransport(ScheduleContext context) {
        forEach(i -> i.afterTransport(context));
    }

    public void afterSchedule(ScheduleContext context, JResponse response) {
        forEach(i -> i.afterSchedule(context, response));
    }

    /* ================== 异常流程 ================== */

    public void onTransportFailure(ScheduleContext context, Throwable throwable) {
        forEach(i -> i.onTransportFailure(context, throwable));
    }

    public void onScheduleFailure(ScheduleContext context, JResponse response, Throwable throwable) {
        forEach(i -> i.onScheduleFailure(context, response, throwable));
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
