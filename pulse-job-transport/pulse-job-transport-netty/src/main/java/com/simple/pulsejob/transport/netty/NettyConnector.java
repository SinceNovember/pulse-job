package com.simple.pulsejob.transport.netty;

import java.util.Collection;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadFactory;

import com.simple.pulsejob.common.JConstants;
import com.simple.pulsejob.common.concurrent.JNamedThreadFactory;
import com.simple.pulsejob.common.util.ClassUtil;
import com.simple.pulsejob.common.util.Maps;
import com.simple.pulsejob.common.util.Requires;
import com.simple.pulsejob.transport.*;
import com.simple.pulsejob.transport.channel.JChannelGroup;
import com.simple.pulsejob.transport.netty.channel.NettyChannelGroup;
import com.simple.pulsejob.transport.netty.estimator.JMessageSizeEstimator;
import com.simple.pulsejob.transport.processor.ConsumerProcessor;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.util.HashedWheelTimer;

public abstract class NettyConnector implements JConnector<JConnection> {

    static {
        // touch off DefaultChannelId.<clinit>
        // because getProcessId() sometimes too slow
        ClassUtil.initializeClass("io.netty.channel.DefaultChannelId", 500);
    }

    protected final Protocol protocol;

    protected final HashedWheelTimer timer = new HashedWheelTimer(new JNamedThreadFactory("connector.timer", true));

    private final ConcurrentMap<UnresolvedAddress, JChannelGroup> addressGroups = Maps.newConcurrentMap();

    private final JConnectionManager connectionManager = new JConnectionManager();

    private Bootstrap bootstrap;

    private EventLoopGroup worker;

    private int nWorkers;

    private ConsumerProcessor processor;

    public NettyConnector(Protocol protocol) {
        this(protocol, JConstants.AVAILABLE_PROCESSORS << 1);
    }

    public NettyConnector(Protocol protocol, int nWorkers) {
        this.protocol = protocol;
        this.nWorkers = nWorkers;
    }

    protected void init() {
        ThreadFactory workerFactory = workerThreadFactory();
        worker = initEventLoopGroup(nWorkers, workerFactory);

        bootstrap = new Bootstrap().group(worker);

        JConfig child = config();
        child.setOption(JOption.IO_RATIO, 100);

        doInit();
    }

    protected abstract void doInit();


    protected ThreadFactory workerThreadFactory() {
        return new JNamedThreadFactory("connector", Thread.MAX_PRIORITY);
    }

    @Override
    public Protocol protocol() {
        return protocol;
    }

    @Override
    public ConsumerProcessor processor() {
        return processor;
    }

    @Override
    public void withProcessor(ConsumerProcessor processor) {
        setProcessor(this.processor = processor);
    }

    @Override
    public JChannelGroup group(UnresolvedAddress address) {
        Requires.requireNotNull(address, "address");

        JChannelGroup group = addressGroups.get(address);
        if (group == null) {
            JChannelGroup newGroup = channelGroup(address);
            group = addressGroups.putIfAbsent(address, newGroup);
            if (group == null) {
                group = newGroup;
            }
        }
        return group;
    }

    @Override
    public Collection<JChannelGroup> groups() {
        return addressGroups.values();
    }

    @Override
    public JConnectionManager connectionManager() {
        return connectionManager;
    }

    @Override
    public void shutdownGracefully() {
        connectionManager.cancelAllAutoReconnect();
        worker.shutdownGracefully().syncUninterruptibly();
        timer.stop();
        if (processor != null) {
            processor.shutdown();
        }
    }

    protected void setOptions() {
        JConfig child = config();

        setIoRatio(child.getOption(JOption.IO_RATIO));

        bootstrap.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, JMessageSizeEstimator.DEFAULT);
    }

    /**
     * A {@link Bootstrap} that makes it easy to bootstrap a {@link io.netty.channel.Channel} to use
     * for clients.
     */
    protected Bootstrap bootstrap() {
        return bootstrap;
    }

    /**
     * Lock object with bootstrap.
     */
    protected Object bootstrapLock() {
        return bootstrap;
    }

    protected EventLoopGroup worker() {
        return worker;
    }

    protected JChannelGroup channelGroup(UnresolvedAddress address) {
        return new NettyChannelGroup(address);
    }

    /**
     * Sets consumer's processor.
     */
    @SuppressWarnings("unused")
    protected void setProcessor(ConsumerProcessor processor) {
        // the default implementation does nothing
    }

    protected WriteBufferWaterMark createWriteBufferWaterMark(int bufLowWaterMark, int bufHighWaterMark) {
        WriteBufferWaterMark waterMark;
        if (bufLowWaterMark >= 0 && bufHighWaterMark > 0) {
            waterMark = new WriteBufferWaterMark(bufLowWaterMark, bufHighWaterMark);
        } else {
            waterMark = new WriteBufferWaterMark(512 * 1024, 1024 * 1024);
        }
        return waterMark;
    }

    /**
     * Sets the percentage of the desired amount of time spent for I/O in the child event loops.
     * The default value is {@code 50}, which means the event loop will try to spend the same
     * amount of time for I/O as for non-I/O tasks.
     */
    public abstract void setIoRatio(int workerIoRatio);

    protected abstract EventLoopGroup initEventLoopGroup(int nThreads, ThreadFactory tFactory);

}
