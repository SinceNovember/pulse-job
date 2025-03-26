package com.simple.pulsejob.common.concurrent.executor;

public interface ExecutorFactory {

    String EXECUTOR_FACTORY_DISRUPTOR = "disruptor";

    CloseableExecutor newExecutor(int coreWorkers, int maxWorkers, int queueCapacity);

    static ExecutorFactory getExecutorFactory(String executor) {
        if (EXECUTOR_FACTORY_DISRUPTOR.equals(executor)) {
            return new DisruptorExecutorFactory();
        }
        return new ThreadPoolExecutorFactory();
    }
    String CONSUMER_EXECUTOR_CORE_WORKERS           = "jupiter.executor.factory.consumer.core.workers";
    String PROVIDER_EXECUTOR_CORE_WORKERS           = "jupiter.executor.factory.provider.core.workers";
    String CONSUMER_EXECUTOR_MAX_WORKERS            = "jupiter.executor.factory.consumer.max.workers";
    String PROVIDER_EXECUTOR_MAX_WORKERS            = "jupiter.executor.factory.provider.max.workers";
    String CONSUMER_EXECUTOR_QUEUE_TYPE             = "jupiter.executor.factory.consumer.queue.type";
    String PROVIDER_EXECUTOR_QUEUE_TYPE             = "jupiter.executor.factory.provider.queue.type";
    String CONSUMER_EXECUTOR_QUEUE_CAPACITY         = "jupiter.executor.factory.consumer.queue.capacity";
    String PROVIDER_EXECUTOR_QUEUE_CAPACITY         = "jupiter.executor.factory.provider.queue.capacity";
    String CONSUMER_DISRUPTOR_WAIT_STRATEGY_TYPE    = "jupiter.executor.factory.consumer.disruptor.wait.strategy.type";
    String PROVIDER_DISRUPTOR_WAIT_STRATEGY_TYPE    = "jupiter.executor.factory.provider.disruptor.wait.strategy.type";
    String CONSUMER_THREAD_POOL_REJECTED_HANDLER    = "jupiter.executor.factory.consumer.thread.pool.rejected.handler";
    String PROVIDER_THREAD_POOL_REJECTED_HANDLER    = "jupiter.executor.factory.provider.thread.pool.rejected.handler";
    String EXECUTOR_AFFINITY_THREAD                 = "jupiter.executor.factory.affinity.thread";

}
