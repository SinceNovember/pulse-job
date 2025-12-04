//package com.simple.pulsejob.admin.scheduler.filter;
//
//import com.simple.pulsejob.admin.scheduler.ScheduleContext;
//import com.simple.pulsejob.admin.scheduler.interceptor.JobInterceptor;
//import com.simple.pulsejob.common.util.Reflects;
//import com.simple.pulsejob.common.util.StackTraceUtil;
//import com.simple.pulsejob.transport.JRequest;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.core.annotation.Order;
//import org.springframework.stereotype.Component;
//import org.springframework.util.CollectionUtils;
//
//import java.util.List;
//
//@Slf4j
//@Order(98)
//@Component
//@RequiredArgsConstructor
//public class InterceptorsFilter implements JobFilter {
//
//    private final List<JobInterceptor> interceptors;
//
//    @Override
//    public void doFilter(JRequest request, ScheduleContext context, JobFilterChain next) throws Throwable {
//        if (CollectionUtils.isEmpty(interceptors)) {
//            next.doFilter(request, context);
//        }
//
//        handleBeforeSchedule(request, context);
//        try {
//            if (next != null) {
//                next.doFilter(request, context);
//            }
//        } finally {
//            handleAfterSchedule(request, context);
//        }
//    }
//
//    private void handleBeforeSchedule(JRequest request, ScheduleContext context) {
//        for (JobInterceptor interceptor : interceptors) {
//            try {
//                interceptor.beforeInvoke(request, context);
//            } catch (Throwable t) {
//                log.error("Interceptor[{}#afterInvoke]: {}.", Reflects.simpleClassName(interceptor),
//                        StackTraceUtil.stackTrace(t));
//            }
//        }
//    }
//
//    private void handleAfterSchedule(JRequest request, ScheduleContext context) {
//        for (int i = interceptors.size() - 1; i >= 0; i--) {
//            try {
//                interceptors.get(i).afterInvoke(request, context);
//            } catch (Throwable t) {
//                log.error("Interceptor[{}#afterInvoke]: {}.", Reflects.simpleClassName(interceptors.get(i)),
//                        StackTraceUtil.stackTrace(t));
//            }
//        }
//    }
//}
