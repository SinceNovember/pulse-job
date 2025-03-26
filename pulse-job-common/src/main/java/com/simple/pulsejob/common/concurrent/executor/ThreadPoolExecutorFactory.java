package com.simple.pulsejob.common.concurrent.executor;

import java.lang.reflect.Constructor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import com.simple.pulsejob.common.JConstants;
import com.simple.pulsejob.common.concurrent.executor.reject.RejectedTaskPolicyWithReport;
import com.simple.pulsejob.common.util.StackTraceUtil;
import com.simple.pulsejob.common.util.StringUtil;
import com.simple.pulsejob.common.util.SystemPropertyUtil;
import com.simple.pulsejob.common.util.internal.logging.InternalLogger;
import com.simple.pulsejob.common.util.internal.logging.InternalLoggerFactory;

public class ThreadPoolExecutorFactory extends AbstractExecutorFactory {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ThreadPoolExecutorFactory.class);

    @Override
    public CloseableExecutor newExecutor(int coreWorkers, int maxWorkers, int queueCapacity) {
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            coreWorkers,
            maxWorkers,
            120L,
            TimeUnit.SECONDS,
            workQueue(queueCapacity),
            threadFactory(),
            createRejectedPolicy(
                new RejectedTaskPolicyWithReport("thread pool", JConstants.DEFAULT_THREAD_NAME_PREFIX)));
        return new CloseableExecutor() {
            @Override
            public void execute(Runnable task) {
                executor.execute(task);
            }

            @Override
            public void shutdown() {
                logger.warn("ThreadPoolExecutorFactory#{} shutdown.", executor);
                executor.shutdownNow();
            }
        };
    }


    private RejectedExecutionHandler createRejectedPolicy(RejectedExecutionHandler defaultHandler) {
        RejectedExecutionHandler handler = null;
        String handlerClass = SystemPropertyUtil.get(CONSUMER_THREAD_POOL_REJECTED_HANDLER);
        ;

        if (StringUtil.isNotBlank(handlerClass)) {
            try {
                Class<?> cls = Class.forName(handlerClass);
                try {
                    Constructor<?> constructor = cls.getConstructor(String.class, String.class);
                    handler = (RejectedExecutionHandler) constructor.newInstance(JConstants.DEFAULT_THREAD_NAME_PREFIX,
                        "thead pool");
                } catch (NoSuchMethodException e) {
                    handler = (RejectedExecutionHandler) cls.newInstance();
                }
            } catch (Exception e) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Construct {} failed, {}.", handlerClass, StackTraceUtil.stackTrace(e));
                }
            }
        }

        return handler == null ? defaultHandler : handler;
    }
}
