package com.simple.pulsejob.transport.netty;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ThreadFactory;
import com.simple.pulsejob.common.JConstants;
import com.simple.pulsejob.common.concurrent.JNamedThreadFactory;
import com.simple.pulsejob.transport.JAcceptor;
import com.simple.pulsejob.transport.JConfig;
import com.simple.pulsejob.transport.JOption;
import com.simple.pulsejob.transport.netty.estimator.JMessageSizeEstimator;
import com.simple.pulsejob.transport.processor.AcceptorProcessor;
import com.simple.pulsejob.transport.processor.ConnectorProcessor;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.DefaultThreadFactory;

public abstract class NettyAcceptor implements JAcceptor {

    protected final Protocol protocol;

    protected final SocketAddress localAddress;

    protected final HashedWheelTimer timer = new HashedWheelTimer(new JNamedThreadFactory("acceptor.timer", true));

    private final int nBosses;
    private final int nWorkers;

    private ServerBootstrap bootstrap;
    private EventLoopGroup boss;
    private EventLoopGroup worker;

    private AcceptorProcessor processor;

    public NettyAcceptor(Protocol protocol, SocketAddress localAddress) {
        this(protocol, localAddress, JConstants.AVAILABLE_PROCESSORS << 1);
    }

    public NettyAcceptor(Protocol protocol, SocketAddress localAddress, int nWorkers) {
        this(protocol, localAddress, 1, nWorkers);
    }

    public NettyAcceptor(Protocol protocol, SocketAddress localAddress, int nBosses, int nWorkers) {
        this.protocol = protocol;
        this.localAddress = localAddress;
        this.nBosses = nBosses;
        this.nWorkers = nWorkers;
    }

    protected void init() {
        ThreadFactory bossFactory = bossThreadFactory("acceptor.boss");
        ThreadFactory workerFactory = workerThreadFactory("acceptor.worker");
        boss = initEventLoopGroup(nBosses, bossFactory);
        worker = initEventLoopGroup(nWorkers, workerFactory);
        bootstrap = new ServerBootstrap().group(boss, worker);

        // parent options
        JConfig parent = configGroup().parent();
        parent.setOption(JOption.IO_RATIO, 100);

        // child options
        JConfig child = configGroup().child();
        child.setOption(JOption.IO_RATIO, 100);
    }

    @Override
    public Protocol protocol() {
        return protocol;
    }

    @Override
    public SocketAddress localAddress() {
        return localAddress;
    }

    @Override
    public int boundPort() {
        if (!(localAddress instanceof InetSocketAddress)) {
            throw new UnsupportedOperationException("Unsupported address type to get port");
        }
        return ((InetSocketAddress) localAddress).getPort();
    }

    @Override
    public AcceptorProcessor processor() {
        return processor;
    }

    @Override
    public void withProcessor(AcceptorProcessor processor) {
        setProcessor(this.processor = processor);
    }

    @Override
    public void shutdownGracefully() {
        boss.shutdownGracefully().syncUninterruptibly();
        worker.shutdownGracefully().syncUninterruptibly();
        timer.stop();
        if (processor != null) {
            processor.shutdown();
        }
    }

    protected ThreadFactory bossThreadFactory(String name) {
        return new JNamedThreadFactory(name, Thread.MAX_PRIORITY);
    }

    @SuppressWarnings("SameParameterValue")
    protected ThreadFactory workerThreadFactory(String name) {
        return new DefaultThreadFactory(name, Thread.MAX_PRIORITY);
    }

    protected void setOptions() {
        JConfig parent = configGroup().parent(); // parent options
        JConfig child = configGroup().child(); // child options

        setIoRatio(parent.getOption(JOption.IO_RATIO), child.getOption(JOption.IO_RATIO));

        bootstrap.childOption(ChannelOption.MESSAGE_SIZE_ESTIMATOR, JMessageSizeEstimator.DEFAULT);
    }

    /**
     * Which allows easy bootstrap of {@link io.netty.channel.ServerChannel}.
     */
    protected ServerBootstrap bootstrap() {
        return bootstrap;
    }

    /**
     * The {@link EventLoopGroup} which is used to handle all the events for the to-be-creates
     * {@link io.netty.channel.Channel}.
     */
    protected EventLoopGroup boss() {
        return boss;
    }

    /**
     * The {@link EventLoopGroup} for the child. These {@link EventLoopGroup}'s are used to
     * handle all the events and IO for {@link io.netty.channel.Channel}'s.
     */
    protected EventLoopGroup worker() {
        return worker;
    }

    /**
     * Sets provider's processor.
     */
    @SuppressWarnings("unused")
    protected void setProcessor(AcceptorProcessor processor) {
        // the default implementation does nothing
    }

    /**
     * Create a WriteBufferWaterMark is used to set low water mark and high water mark for the write buffer.
     */
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
    public abstract void setIoRatio(int bossIoRatio, int workerIoRatio);

    /**
     * Create a new {@link io.netty.channel.Channel} and bind it.
     */
    protected abstract ChannelFuture bind(SocketAddress localAddress);

    /**
     * Create a new instance using the specified number of threads, the given {@link ThreadFactory}.
     */
    protected abstract EventLoopGroup initEventLoopGroup(int nThreads, ThreadFactory tFactory);

}
