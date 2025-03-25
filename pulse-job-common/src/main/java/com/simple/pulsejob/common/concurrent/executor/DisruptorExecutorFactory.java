package com.simple.pulsejob.common.concurrent.executor;

import java.lang.annotation.Target;
import com.simple.pulsejob.common.JConstants;
import com.simple.pulsejob.common.concurrent.executor.disruptor.TaskDispatcher;
import com.simple.pulsejob.common.concurrent.executor.disruptor.WaitStrategyType;
import com.simple.pulsejob.common.util.SystemPropertyUtil;
import com.simple.pulsejob.common.util.internal.logging.InternalLogger;
import com.simple.pulsejob.common.util.internal.logging.InternalLoggerFactory;

public class DisruptorExecutorFactory extends AbstractExecutorFactory {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DisruptorExecutorFactory.class);

    @Override
    public CloseableExecutor newExecutor(int coreWorkers, int maxWorkers, int queueCapacity) {
        final TaskDispatcher executor = new TaskDispatcher(
            coreWorkers,
            threadFactory(),
            queueCapacity,
            maxWorkers,
            waitStrategyType(WaitStrategyType.LITE_BLOCKING_WAIT),
            JConstants.DEFAULT_THREAD_NAME_PREFIX);
        return new CloseableExecutor() {
            @Override
            public void execute(Runnable task) {
                executor.execute(task);
            }

            @Override
            public void shutdown() {
                logger.warn("DisruptorExecutorFactory#{} shutdown.", executor);
                executor.shutdown();
            }
        };
    }

    @SuppressWarnings("SameParameterValue")
    private WaitStrategyType waitStrategyType(WaitStrategyType defaultType) {
        WaitStrategyType strategyType =
            WaitStrategyType.parse(SystemPropertyUtil.get(CONSUMER_DISRUPTOR_WAIT_STRATEGY_TYPE));
        return strategyType == null ? defaultType : strategyType;
    }
}
