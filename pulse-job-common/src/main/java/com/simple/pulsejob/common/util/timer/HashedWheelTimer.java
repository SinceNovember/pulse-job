package com.simple.pulsejob.common.util.timer;

import com.simple.pulsejob.common.JConstants;
import com.simple.pulsejob.common.concurrent.JNamedThreadFactory;
import com.simple.pulsejob.common.util.internal.Platform;

import java.util.Collections;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLong;

public class HashedWheelTimer implements Timer {

    private static final AtomicIntegerFieldUpdater<HashedWheelTimer> workerStateUpdater =
            AtomicIntegerFieldUpdater.newUpdater(HashedWheelTimer.class, "workerState");

    private final Thread workerThread;

    public static final int WORKER_STATE_INIT = 0;
    public static final int WORKER_STATE_STARTED = 1;
    public static final int WORKER_STATE_SHUTDOWN = 2;
    @SuppressWarnings({"unused", "FieldMayBeFinal"})
    private volatile int workerState; // 0 - init, 1 - started, 2 - shut down

    private final long tickDuration;
    private final HashedWheelBucket[] wheel;
    private final int mask;
    private final CountDownLatch startTimeInitialized = new CountDownLatch(1);
    private final Queue<HashedWheelTimeout> timeouts = new ConcurrentLinkedQueue<>();
    private final Queue<HashedWheelTimeout> cancelledTimeouts = new ConcurrentLinkedQueue<>();
    private final AtomicLong pendingTimeouts = new AtomicLong(0);
    private final long maxPendingTimeouts;

    private volatile long startTime;
    private final Executor executor;

    public HashedWheelTimer() {
        this(new JNamedThreadFactory());
    }

    public HashedWheelTimer(long tickDuration, TimeUnit unit) {
        this(Executors.defaultThreadFactory(), tickDuration, unit);
    }

    public HashedWheelTimer(long tickDuration, TimeUnit unit, int ticksPerWheel) {
        this(Executors.defaultThreadFactory(), tickDuration, unit, ticksPerWheel);
    }


    public HashedWheelTimer(ThreadFactory threadFactory) {
        this(threadFactory, 100, TimeUnit.MILLISECONDS);
    }


    public HashedWheelTimer(
            ThreadFactory threadFactory, long tickDuration, TimeUnit unit) {
        this(threadFactory, tickDuration, unit, 512);
    }

    public HashedWheelTimer(
            ThreadFactory threadFactory, long tickDuration, TimeUnit unit, int ticksPerWheel) {
        this(threadFactory, tickDuration, unit, ticksPerWheel, -1, null);
    }

    public HashedWheelTimer(
            ThreadFactory threadFactory, long tickDuration, TimeUnit unit, int ticksPerWheel, long maxPendingTimeouts) {
        this(threadFactory, tickDuration, unit, ticksPerWheel, maxPendingTimeouts, null);
    }

    public HashedWheelTimer(
            ThreadFactory threadFactory, long tickDuration, TimeUnit unit, int ticksPerWheel, long maxPendingTimeouts, Executor executor) {
        if (threadFactory == null) {
            throw new NullPointerException("threadFactory");
        }
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        if (tickDuration <= 0) {
            throw new IllegalArgumentException("tickDuration must be greater than 0: " + tickDuration);
        }
        if (ticksPerWheel <= 0) {
            throw new IllegalArgumentException("ticksPerWheel must be greater than 0: " + ticksPerWheel);
        }
        //规范时间轮的大小，并创建时间轮数组
        wheel = createWheel(ticksPerWheel);
        mask = wheel.length - 1;

        this.executor = executor != null ? executor : JConstants.createDefaultExecutor();

        //将调度时间转换为纳秒
        this.tickDuration = unit.toNanos(tickDuration);
        if (this.tickDuration >= Long.MAX_VALUE / wheel.length) {
            throw new IllegalArgumentException(String.format(
                    "tickDuration: %d (expected: 0 < tickDuration in nanos < %d",
                    tickDuration, Long.MAX_VALUE / wheel.length));
        }
        Worker worker = new Worker();
        workerThread = threadFactory.newThread(worker);

        this.maxPendingTimeouts = maxPendingTimeouts;

    }

    private static HashedWheelBucket[] createWheel(int ticksPerWheel) {
        if (ticksPerWheel <= 0) {
            throw new IllegalArgumentException(
                    "ticksPerWheel must be greater than 0: " + ticksPerWheel);
        }
        if (ticksPerWheel > 1073741824) {
            throw new IllegalArgumentException(
                    "ticksPerWheel may not be greater than 2^30: " + ticksPerWheel);
        }

        //将轮盘大小规范化为大于等于它的最小 2 的幂
        ticksPerWheel = normalizeTicksPerWheel(ticksPerWheel);
        HashedWheelBucket[] wheel = new HashedWheelBucket[ticksPerWheel];
        for (int i = 0; i < wheel.length; i++) {
            wheel[i] = new HashedWheelBucket();
        }
        return wheel;
    }

    private static int normalizeTicksPerWheel(int ticksPerWheel) {
        // 最高位的 1 变成全 1，然后 +1，得到 >= ticksPerWheel 的最小 2 的幂
        ticksPerWheel--;
        ticksPerWheel |= ticksPerWheel >>> 1;
        ticksPerWheel |= ticksPerWheel >>> 2;
        ticksPerWheel |= ticksPerWheel >>> 4;
        ticksPerWheel |= ticksPerWheel >>> 8;
        ticksPerWheel |= ticksPerWheel >>> 16;
        return ticksPerWheel + 1;
    }

    @Override
    public Timeout newTimeout(TimerTask task, long delay, TimeUnit unit) {
        if (task == null) {
            throw new NullPointerException("task");
        }
        if (unit == null) {
            throw new NullPointerException("unit");
        }

        long pendingTimeoutsCount = pendingTimeouts.incrementAndGet();

        if (maxPendingTimeouts > 0 && pendingTimeoutsCount > maxPendingTimeouts) {
            pendingTimeouts.decrementAndGet();
            throw new RejectedExecutionException("Number of pending timeouts ("
                    + pendingTimeoutsCount + ") is greater than or equal to maximum allowed pending "
                    + "timeouts (" + maxPendingTimeouts + ")");
        }
        start();

        //计算过期时间差额
        long deadline = System.nanoTime() + unit.toNanos(delay) - startTime;

        if (delay > 0 && deadline < 0) {
            deadline = Long.MAX_VALUE;
        }
        HashedWheelTimeout timeout = new HashedWheelTimeout(this, task, deadline);
        timeouts.add(timeout);
        return timeout;
    }

    public void start() {
        switch (workerStateUpdater.get(this)) {
            case WORKER_STATE_INIT:
                if (workerStateUpdater.compareAndSet(this, WORKER_STATE_INIT, WORKER_STATE_STARTED)) {
                    workerThread.start();
                }
                break;
            case WORKER_STATE_STARTED:
                break;
            case WORKER_STATE_SHUTDOWN:
                throw new IllegalStateException("cannot be started once stopped");
            default:
                throw new Error("Invalid WorkerState");
        }

        // 工作线程还未启动的话挂起当前任务进行等待
        while (startTime == 0) {
            try {
                startTimeInitialized.await();
            } catch (InterruptedException ignore) {
                // Ignore - it will be ready very soon.
            }
        }
    }

    public long pendingTimeouts() {
        return pendingTimeouts.get();
    }


    private final class Worker implements Runnable {

        private final Set<Timeout> unprocessedTimeouts = new HashSet<>();

        private long tick;

        @Override
        public void run() {
            //初始化时间轮的开始时间
            startTime = System.nanoTime();
            if (startTime == 0) {
                startTime = 1;
            }

            startTimeInitialized.countDown();

            do {
                final long deadline = waitForNextTick();
                if (deadline > 0) {
                    //取余后取轮盘的索引
                    int idx = (int) (tick & mask);
                    //将取消掉（取消队列中）的任务从各自的bucket中移除掉
                    processCancelledTasks();
                    HashedWheelBucket bucket = wheel[idx];
                    //将待添加的队列的任务，进行计算所在的圈数以及所在的bucket位置后添加到对应的bucket中
                    transferTimeoutsToBuckets();
                    //将当前同桶里过期的任务剔除掉
                    bucket.expireTimeouts(deadline);
                    tick++;
                }
            } while (workerStateUpdater.get(HashedWheelTimer.this) == WORKER_STATE_STARTED);

            //时间轮worker停止工作，将未处理的任务添加到unprocessedTimeouts中
            for (HashedWheelBucket bucket : wheel) {
                bucket.clearTimeouts(unprocessedTimeouts);
            }
            //将还没推到时间轮里的任务也追加unprocessedTimeout队列中
            for (;;) {
                HashedWheelTimeout timeout = timeouts.poll();
                if (timeout == null) {
                    break;
                }
                if (!timeout.isCancelled()) {
                    unprocessedTimeouts.add(timeout);
                }
            }
            processCancelledTasks();
        }

        private void transferTimeoutsToBuckets() {
            for (int i = 0; i < 100000; i++) {
                HashedWheelTimeout timeout = timeouts.poll();
                if (timeout == null) {
                    // all processed
                    break;
                }
                if (timeout.state() == HashedWheelTimeout.ST_CANCELLED) {
                    // Was cancelled in the meantime.
                    continue;
                }

                long calculated = timeout.deadline / tickDuration;
                timeout.remainingRounds = (calculated - tick) / wheel.length;

                final long ticks = Math.max(calculated, tick); // Ensure we don't schedule for past.
                int stopIndex = (int) (ticks & mask);

                HashedWheelBucket bucket = wheel[stopIndex];
                bucket.addTimeout(timeout);
            }

        }

        private long waitForNextTick() {
            long deadline = tickDuration * (tick + 1);

            for (; ; ) {
                final long currentTime = System.nanoTime() - startTime;
                long sleepTimeMs = (deadline - currentTime + 999999) / 1000000;

                if (sleepTimeMs <= 0) {
                    if (currentTime == Long.MIN_VALUE) {
                        return -Long.MAX_VALUE;
                    } else {
                        return currentTime;
                    }
                }

                // Check if we run on windows, as if thats the case we will need
                // to round the sleepTime as workaround for a bug that only affect
                // the JVM if it runs on windows.
                //
                // See https://github.com/netty/netty/issues/356
                if (Platform.isWindows()) {
                    sleepTimeMs = sleepTimeMs / 10 * 10;
                }

                try {
                    Thread.sleep(sleepTimeMs);
                } catch (InterruptedException ignored) {
                    if (workerStateUpdater.get(HashedWheelTimer.this) == WORKER_STATE_SHUTDOWN) {
                        return Long.MIN_VALUE;
                    }
                }
            }
        }

        /**
         * 把取消掉的任务从Bucket桶里移除掉
         */
        private void processCancelledTasks() {
            for (; ; ) {
                HashedWheelTimeout timeout = cancelledTimeouts.poll();
                if (timeout == null) {
                    // all processed
                    break;
                }
                try {
                    timeout.remove();
                } catch (Throwable t) {

                }
            }
        }

        public Set<Timeout> unprocessedTimeouts() {
            return Collections.unmodifiableSet(unprocessedTimeouts);
        }
    }

    /**
     * 定时任务实际的包装类，里面包含了当前任务的状态
     */
    private static final class HashedWheelTimeout implements Timeout {

        private static final int ST_INIT = 0;
        private static final int ST_CANCELLED = 1;
        private static final int ST_EXPIRED = 2;

        private static final AtomicIntegerFieldUpdater<HashedWheelTimeout> STATE_UPDATER =
                AtomicIntegerFieldUpdater.newUpdater(HashedWheelTimeout.class, "state");

        private final HashedWheelTimer timer;
        private final TimerTask task;
        private final long deadline;

        @SuppressWarnings({"unused", "FieldMayBeFinal", "RedundantFieldInitialization"})
        private volatile int state = ST_INIT;

        long remainingRounds;

        HashedWheelTimeout next;
        HashedWheelTimeout prev;

        HashedWheelBucket bucket;

        HashedWheelTimeout(HashedWheelTimer timer, TimerTask task, long deadline) {
            this.timer = timer;
            this.task = task;
            this.deadline = deadline;
        }

        @Override
        public Timer timer() {
            return timer;
        }

        @Override
        public TimerTask task() {
            return task;
        }

        @Override
        public boolean cancel() {
            // only update the state it will be removed from HashedWheelBucket on next tick.
            if (!compareAndSetState(ST_INIT, ST_CANCELLED)) {
                return false;
            }
            // If a task should be canceled we put this to another queue which will be processed on each tick.
            // So this means that we will have a GC latency of max. 1 tick duration which is good enough. This way
            // we can make again use of our MpscLinkedQueue and so minimize the locking / overhead as much as possible.
            timer.cancelledTimeouts.add(this);
            return true;
        }

        void remove() {
            HashedWheelBucket bucket = this.bucket;
            if (bucket != null) {
                bucket.remove(this);
            } else {
                timer.pendingTimeouts.decrementAndGet();
            }
        }

        public boolean compareAndSetState(int expected, int state) {
            return STATE_UPDATER.compareAndSet(this, expected, state);
        }

        public int state() {
            return state;
        }

        @Override
        public boolean isCancelled() {
            return state() == ST_CANCELLED;
        }

        @Override
        public boolean isExpired() {
            return state() == ST_EXPIRED;
        }

        public void expire() {
            if (!compareAndSetState(ST_INIT, ST_EXPIRED)) {
                return;
            }
            timer.executor.execute(() -> {
                try {
                    task.run(this);
                } catch (Throwable t) {
                    System.out.println("run error");
                }
            });
        }

        @Override
        public String toString() {
            final long currentTime = System.nanoTime();
            long remaining = deadline - currentTime + timer.startTime;

            StringBuilder buf = new StringBuilder(192)
                    .append(this.getClass())
                    .append('(')
                    .append("deadline: ");
            if (remaining > 0) {
                buf.append(remaining)
                        .append(" ns later");
            } else if (remaining < 0) {
                buf.append(-remaining)
                        .append(" ns ago");
            } else {
                buf.append("now");
            }

            if (isCancelled()) {
                buf.append(", cancelled");
            }

            return buf.append(", task: ")
                    .append(task())
                    .append(')')
                    .toString();
        }
    }

    /**
     * 时间轮的桶结构，里面包含了一个双向链表用来存储定时任务
     */
    private static final class HashedWheelBucket {
        private HashedWheelTimeout head;

        private HashedWheelTimeout tail;

        /**
         * 添加任务到该桶链表的最后
         */
        public void addTimeout(HashedWheelTimeout timeout) {
            assert timeout.bucket == null;
            timeout.bucket = this;
            if (head == null) {
                head = tail = timeout;
            } else {
                tail.next = timeout;
                timeout.prev = tail;
                tail = timeout;
            }
        }

        /**
         * 过期桶里所有给定时间点之前的任务
         */
        public void expireTimeouts(long deadline) {
            HashedWheelTimeout timeout = head;

            while (timeout != null) {
                HashedWheelTimeout next = timeout.next;
                if (timeout.remainingRounds <= 0) {
                    next = remove(timeout);
                    if (timeout.deadline <= deadline) {
                        timeout.expire();
                    } else {
                        // The timeout was placed into a wrong slot. This should never happen.
                        throw new IllegalStateException(String.format(
                                "timeout.deadline (%d) > deadline (%d)", timeout.deadline, deadline));
                    }
                } else if (timeout.isCancelled()) {
                    next = remove(timeout);
                } else {
                    //当前任务的时间轮数减去一圈
                    timeout.remainingRounds--;
                }
                timeout = next;
            }
        }

        public HashedWheelTimeout remove(HashedWheelTimeout timeout) {
            HashedWheelTimeout next = timeout.next;
            if (timeout.prev != null) {
                timeout.prev.next = next;
            }
            if (timeout.next != null) {
                timeout.next.prev = timeout.prev;
            }

            if (timeout == head) {
                if (timeout == tail) {
                    tail = null;
                    head = null;
                } else {
                    head = next;
                }
            } else if (timeout == tail) {
                tail = timeout.prev;
            }
            timeout.prev = null;
            timeout.next = null;
            timeout.bucket = null;
            timeout.timer.pendingTimeouts.decrementAndGet();
            return next;
        }

        /**
         * 将该桶中所有的任务从链表中移除并添加到参数的Set中
         */
        public void clearTimeouts(Set<Timeout> set) {
            for (; ; ) {
                HashedWheelTimeout timeout = pollTimeout();
                if (timeout == null) {
                    return;
                }
                if (timeout.isExpired() || timeout.isCancelled()) {
                    continue;
                }
                set.add(timeout);
            }
        }

        private HashedWheelTimeout pollTimeout() {
            HashedWheelTimeout head = this.head;
            if (head == null) {
                return null;
            }
            HashedWheelTimeout next = head.next;
            if (next == null) {
                tail = this.head = null;
            } else {
                this.head = next;
                next.prev = null;
            }

            // null out prev and next to allow for GC.
            head.next = null;
            head.prev = null;
            head.bucket = null;
            return head;
        }
    }
}
