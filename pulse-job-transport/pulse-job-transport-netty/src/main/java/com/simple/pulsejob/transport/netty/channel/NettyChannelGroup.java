package com.simple.pulsejob.transport.netty.channel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import com.simple.pulsejob.common.JConstants;
import com.simple.pulsejob.common.atomic.AtomicUpdater;
import com.simple.pulsejob.common.util.IntSequence;
import com.simple.pulsejob.common.util.Lists;
import com.simple.pulsejob.common.util.SystemClock;
import com.simple.pulsejob.common.util.SystemPropertyUtil;
import com.simple.pulsejob.common.util.ThrowUtil;
import com.simple.pulsejob.transport.UnresolvedAddress;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.channel.JChannelGroup;
import io.netty.channel.ChannelFutureListener;

public class NettyChannelGroup implements JChannelGroup {

    private static long LOSS_INTERVAL = SystemPropertyUtil
        .getLong("jupiter.io.channel.group.loss.interval.millis", TimeUnit.MINUTES.toMillis(5));

    private static int DEFAULT_SEQUENCE_STEP = (JConstants.AVAILABLE_PROCESSORS << 3) + 1;

    private static final AtomicReferenceFieldUpdater<CopyOnWriteArrayList, Object[]> channelsUpdater =
        AtomicUpdater.newAtomicReferenceFieldUpdater(CopyOnWriteArrayList.class, Object[].class, "array");

    private static final AtomicIntegerFieldUpdater<NettyChannelGroup> signalNeededUpdater =
        AtomicIntegerFieldUpdater.newUpdater(NettyChannelGroup.class, "signalNeeded");

    private final ConcurrentLinkedQueue<Runnable> waitAvailableListeners = new ConcurrentLinkedQueue<>();

    private UnresolvedAddress address;

    private final CopyOnWriteArrayList<NettyChannel> channels = new CopyOnWriteArrayList<>();

    // 连接断开时自动被移除
    private final Function<Runnable, ChannelFutureListener> remover =
        preCloseProcessor -> future -> {
            JChannel jChannel = NettyChannel.attachChannel(future.channel());
            if (preCloseProcessor != null) {
                preCloseProcessor.run();
            }
            remove(jChannel);
        };
    private final IntSequence sequence = new IntSequence(DEFAULT_SEQUENCE_STEP);

    private final ReentrantLock lock = new ReentrantLock();

    private final Condition notifyCondition = lock.newCondition();

    private volatile int signalNeeded = 0; // 0: false, 1: true

    private volatile boolean connecting = false;

    private volatile int capacity = Integer.MAX_VALUE;

    private volatile long timestamp = SystemClock.millisClock().now();

    private volatile long deadlineMillis = -1;

    public NettyChannelGroup() {
    }

    public NettyChannelGroup(UnresolvedAddress address) {
        this.address = address;
    }

    @Override
    public UnresolvedAddress remoteAddress() {
        return address;
    }

    @Override
    public JChannel next() {
        for (; ; ) {
            //创建一个当前时间点里所拥有的channels的副本
            Object[] elements = channelsUpdater.get(channels);
            int length = elements.length;
            if (length == 0) {
                if (waitForAvailable(1000)) {
                    continue;
                }
                throw new IllegalStateException("No channel");
            }

            if (length == 1) {
                return (JChannel) elements[0];
            }
            int index = sequence.next() & Integer.MAX_VALUE;
            return (JChannel) elements[index % length];
        }
    }

    @Override
    public List<JChannel> channels() {
        return Lists.newArrayList(channels);
    }

    @Override
    public boolean isEmpty() {
        return channels.isEmpty();
    }

    @Override
    public boolean add(JChannel channel) {
        return add(channel, null);
    }

    @Override
    public boolean add(JChannel channel, Runnable preCloseProcessor) {
        boolean added = channel instanceof NettyChannel && channels.add((NettyChannel) channel);
        if (added) {
            timestamp = SystemClock.millisClock().now();
            ((NettyChannel) channel).channel().closeFuture().addListener(remover.apply(preCloseProcessor));
            deadlineMillis = -1;

            //如果signalNeeded之前的值是1的话，代表需要通知其他线程，新的 channel 已添加
            if (signalNeededUpdater.getAndSet(this, 0) != 0) {
                final ReentrantLock _look = lock;
                _look.lock();
                try {
                    notifyCondition.notifyAll();
                } finally {
                    _look.unlock();
                }
            }
            notifyListeners();
        }
        return added;
    }


    @Override
    public boolean remove(JChannel channel) {
        boolean removed = channel instanceof NettyChannel && channels.remove(channel);
        if (removed) {
            timestamp = SystemClock.millisClock().now(); // reset timestamp

            if (channels.isEmpty()) {
                deadlineMillis = SystemClock.millisClock().now() + LOSS_INTERVAL;
            }
        }
        return removed;
    }
    @Override
    public int size() {
        return channels.size();
    }


    @Override
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public boolean isConnecting() {
        return connecting;
    }

    @Override
    public void setConnecting(boolean connecting) {
        this.connecting = connecting;
    }

    @Override
    public boolean isAvailable() {
        return !channels.isEmpty();
    }
    @Override
    public boolean waitForAvailable(long timeoutMillis) {
        boolean available = isAvailable();
        if (available) {
            return true;
        }
        long remains = TimeUnit.MILLISECONDS.toNanos(timeoutMillis);

        final ReentrantLock _look = lock;
        _look.lock();
        try {
            while (!(available = isAvailable())) {
                signalNeeded = 1;
                //等待有新的Channel加进来之后来唤醒
                if ((remains = notifyCondition.awaitNanos(remains)) <= 0) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            ThrowUtil.throwException(e);
        } finally {
            _look.unlock();
        }
        return available;
    }

    @Override
    public void onAvailable(Runnable listener) {
        waitAvailableListeners.add(listener);
        if (isAvailable()) {
            notifyListeners();
        }
    }

    @Override
    public long timestamp() {
        return timestamp;
    }

    @Override
    public long deadlineMillis() {
        return deadlineMillis;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NettyChannelGroup that = (NettyChannelGroup) o;

        return address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }

    @Override
    public String toString() {
        return "NettyChannelGroup{" +
            "address=" + address +
            ", channels=" + channels +
            ", timestamp=" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ").format(new Date(timestamp)) +
            ", deadlineMillis=" + deadlineMillis +
            '}';
    }

    void notifyListeners() {
        for (; ; ) {
            Runnable listener = waitAvailableListeners.poll();
            if (listener == null) {
                break;
            }
            listener.run();
        }
    }
}
