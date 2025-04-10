package com.simple.pulsejob.common.concurrent.executor.disruptor;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.LiteBlockingWaitStrategy;
import com.lmax.disruptor.LiteTimeoutBlockingWaitStrategy;
import com.lmax.disruptor.PhasedBackoffWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.TimeoutBlockingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.simple.pulsejob.common.concurrent.JNamedThreadFactory;
import com.simple.pulsejob.common.concurrent.executor.CloseableExecutor;
import com.simple.pulsejob.common.concurrent.executor.DisruptorExecutorFactory;
import com.simple.pulsejob.common.concurrent.executor.reject.RejectedTaskPolicyWithReport;
import com.simple.pulsejob.common.util.Pow2;
import com.simple.pulsejob.common.util.Requires;

public class TaskDispatcher implements Dispatcher<Runnable>, Executor {

    private static final EventFactory<MessageEvent<Runnable>> eventFactory = MessageEvent::new;

    private final Disruptor<MessageEvent<Runnable>> disruptor;
    private final ExecutorService reserveExecutor;

    public TaskDispatcher(int numWorkers, ThreadFactory threadFactory) {
        this(numWorkers, threadFactory, BUFFER_SIZE, 0, WaitStrategyType.BLOCKING_WAIT, null);
    }

    public TaskDispatcher(int numWorkers,
                          ThreadFactory threadFactory,
                          int bufSize,
                          int numReserveWorkers,
                          WaitStrategyType waitStrategyType,
                          String dumpPrefixName) {
        Requires.requireTrue(bufSize > 0, "bufSize must be larger than 0");

        if (!Pow2.isPowerOfTwo(bufSize)) {
            bufSize = Pow2.roundToPowerOfTwo(bufSize);
        }
        if (numReserveWorkers > 0) {
            String name = "reserve.processor";

            RejectedExecutionHandler handler;
            if (dumpPrefixName == null) {
                handler = new RejectedTaskPolicyWithReport(name);
            } else {
                handler = new RejectedTaskPolicyWithReport(name, dumpPrefixName);
            }

            reserveExecutor = new ThreadPoolExecutor(
                0,
                numReserveWorkers,
                60L,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new JNamedThreadFactory(name),
                handler);

        } else {
            reserveExecutor = null;
        }
        WaitStrategy waitStrategy;
        switch (waitStrategyType) {
            case BLOCKING_WAIT:
                waitStrategy = new BlockingWaitStrategy();
                break;
            case LITE_BLOCKING_WAIT:
                waitStrategy = new LiteBlockingWaitStrategy();
                break;
            case TIMEOUT_BLOCKING_WAIT:
                waitStrategy = new TimeoutBlockingWaitStrategy(1000, TimeUnit.MILLISECONDS);
                break;
            case LITE_TIMEOUT_BLOCKING_WAIT:
                waitStrategy = new LiteTimeoutBlockingWaitStrategy(1000, TimeUnit.MILLISECONDS);
                break;
            case PHASED_BACK_OFF_WAIT:
                waitStrategy = PhasedBackoffWaitStrategy.withLiteLock(1000, 1000, TimeUnit.NANOSECONDS);
                break;
            case SLEEPING_WAIT:
                waitStrategy = new SleepingWaitStrategy();
                break;
            case YIELDING_WAIT:
                waitStrategy = new YieldingWaitStrategy();
                break;
            case BUSY_SPIN_WAIT:
                waitStrategy = new BusySpinWaitStrategy();
                break;
            default:
                throw new UnsupportedOperationException(waitStrategyType.toString());
        }
        if (threadFactory == null) {
            threadFactory = new JNamedThreadFactory("disruptor.processor");
        }
        Disruptor<MessageEvent<Runnable>> dr =
            new Disruptor<>(eventFactory, bufSize, threadFactory, ProducerType.MULTI, waitStrategy);

        dr.setDefaultExceptionHandler(new LoggingExceptionHandler());
        numWorkers = Math.min(Math.abs(numWorkers), MAX_NUM_WORKERS);
        if (numWorkers == 1) {
            dr.handleEventsWith(new TaskHandler());
        } else {
            TaskHandler[] handlers = new TaskHandler[numWorkers];
            for (int i = 0; i < numWorkers; i++) {
                handlers[i] = new TaskHandler();
            }
            dr.handleEventsWithWorkerPool(handlers);
        }
        dr.start();
        disruptor = dr;
    }

    @Override
    public boolean dispatch(Runnable message) {
        RingBuffer<MessageEvent<Runnable>> ringBuffer = disruptor.getRingBuffer();
        return ringBuffer.tryPublishEvent((event, sequence) -> event.setMessage(message));
    }

    @Override
    public void execute(Runnable message) {
        if (!dispatch(message)) {
            if (reserveExecutor != null) {
                reserveExecutor.execute(message);
            } else {
                throw new RejectedExecutionException("Ring buffer is full");
            }
        }
    }

    @Override
    public void shutdown() {
        disruptor.shutdown();
        if (reserveExecutor != null) {
            reserveExecutor.shutdownNow();
        }
    }

}
