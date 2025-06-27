package com.simple.pulsejob.transport.netty;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import com.simple.pulsejob.common.JConstants;
import com.simple.pulsejob.common.util.Requires;
import com.simple.pulsejob.transport.CodecConfig;
import com.simple.pulsejob.transport.JOption;
import com.simple.pulsejob.transport.UnresolvedAddress;
import com.simple.pulsejob.transport.channel.JChannelGroup;
import com.simple.pulsejob.transport.exception.ConnectFailedException;
import com.simple.pulsejob.transport.netty.handler.IdleStateChecker;
import com.simple.pulsejob.transport.netty.handler.LowCopyProtocolDecoder;
import com.simple.pulsejob.transport.netty.handler.LowCopyProtocolEncoder;
import com.simple.pulsejob.transport.netty.handler.ProtocolDecoder;
import com.simple.pulsejob.transport.netty.handler.ProtocolEncoder;
import com.simple.pulsejob.transport.netty.handler.connector.ConnectionWatchdog;
import com.simple.pulsejob.transport.netty.handler.connector.ConnectorHandler;
import com.simple.pulsejob.transport.netty.handler.connector.ConnectorIdleStateTrigger;
import com.simple.pulsejob.transport.processor.ConnectorProcessor;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.handler.flush.FlushConsolidationHandler;

/**
 * Jupiter tcp connector based on netty.
 *
 * <pre>
 * ************************************************************************
 *                      ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐
 *
 *                 ─ ─ ─│        Server         │─ ─▷
 *                 │                                 │
 *                      └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘
 *                 │                                 ▽
 *                                              I/O Response
 *                 │                                 │
 *
 *                 │                                 │
 * ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─
 * │               │                                 │                │
 *
 * │               │                                 │                │
 *   ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐      ┌ ─ ─ ─ ─ ─ ─▽─ ─ ─ ─ ─ ─ ─
 * │  ConnectionWatchdog#outbound        ConnectionWatchdog#inbound│  │
 *   └ ─ ─ ─ ─ ─ ─ △ ─ ─ ─ ─ ─ ─ ┘      └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─
 * │                                                 │                │
 *                 │
 * │  ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐       ┌ ─ ─ ─ ─ ─ ─▽─ ─ ─ ─ ─ ─ ┐   │
 *     IdleStateChecker#outBound         IdleStateChecker#inBound
 * │  └ ─ ─ ─ ─ ─ ─△─ ─ ─ ─ ─ ─ ┘       └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘   │
 *                                                   │
 * │               │                                                  │
 *                                      ┌ ─ ─ ─ ─ ─ ─▽─ ─ ─ ─ ─ ─ ┐
 * │               │                     ConnectorIdleStateTrigger    │
 *                                      └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘
 * │               │                                 │                │
 *
 * │               │                    ┌ ─ ─ ─ ─ ─ ─▽─ ─ ─ ─ ─ ─ ┐   │
 *                                            ProtocolDecoder
 * │               │                    └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘   │
 *                                                   │
 * │               │                                                  │
 *                                      ┌ ─ ─ ─ ─ ─ ─▽─ ─ ─ ─ ─ ─ ┐
 * │               │                         ConnectorHandler         │
 *    ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐       └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘
 * │        ProtocolEncoder                          │                │
 *    └ ─ ─ ─ ─ ─ ─△─ ─ ─ ─ ─ ─ ┘
 * │                                                 │                │
 * ─ ─ ─ ─ ─ ─ ─ ─ ┼ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─
 *                                       ┌ ─ ─ ─ ─ ─ ▽ ─ ─ ─ ─ ─ ┐
 *                 │
 *                                       │       Processor       │
 *                 │
 *            I/O Request                └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘
 *
 * </pre>
 * <p>
 * jupiter
 * org.jupiter.transport.netty
 *
 * @author jiachun.fjc
 */
public class JNettyTcpConnector extends NettyTcpConnector {

    private final ConnectorIdleStateTrigger idleStateTrigger = new ConnectorIdleStateTrigger();
    private final ChannelOutboundHandler encoder =
        CodecConfig.isCodecLowCopy() ? new LowCopyProtocolEncoder() : new ProtocolEncoder();
    private final ConnectorHandler handler = new ConnectorHandler();

    public JNettyTcpConnector() {
        super();
    }

    public JNettyTcpConnector(boolean isNative) {
        super(isNative);
    }

    public JNettyTcpConnector(int nWorkers) {
        super(nWorkers);
    }

    public JNettyTcpConnector(int nWorkers, boolean isNative) {
        super(nWorkers, isNative);
    }

    @Override
    protected void doInit() {
        // child options
        config().setOption(JOption.SO_REUSEADDR, true);
        config().setOption(JOption.CONNECT_TIMEOUT_MILLIS, (int) TimeUnit.SECONDS.toMillis(3));
        initChannelFactory();
    }

    @Override
    protected void setProcessor(ConnectorProcessor processor) {
        handler.processor(Requires.requireNotNull(processor, "processor"));
    }


    @Override
    public JNettyConnection connect(UnresolvedAddress address, boolean async) {
        setOptions();

        final Bootstrap boot = bootstrap();
        final SocketAddress socketAddress = InetSocketAddress.createUnresolved(address.getHost(), address.getPort());
        JChannelGroup group = group(address);

        final ConnectionWatchdog watchdog = new ConnectionWatchdog(boot, timer, socketAddress, group) {
            @Override
            public ChannelHandler[] handlers() {
                return new ChannelHandler[] {
                    new FlushConsolidationHandler(JConstants.EXPLICIT_FLUSH_AFTER_FLUSHES, true),
                    this,
                    new IdleStateChecker(timer, 0, JConstants.WRITER_IDLE_TIME_SECONDS, 0),
                    idleStateTrigger,
                    CodecConfig.isCodecLowCopy() ? new LowCopyProtocolDecoder() : new ProtocolDecoder(),
                    encoder,
                    handler
                };
            }
        };

        ChannelFuture future;
        try {
            synchronized (bootstrapLock()) {
                boot.handler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast(watchdog.handlers());
                    }
                });
                future = boot.connect(socketAddress);
            }
            if (!async) {
                future.sync();
            }
        } catch (Throwable t) {
            throw new ConnectFailedException("Connects to [" + address + "] fails", t);
        }

        return new JNettyConnection(address, future) {
            @Override
            public void setReconnect(boolean reconnect) {
                if (reconnect) {
                    watchdog.start();
                } else {
                    watchdog.stop();
                }
            }
        };
    }

}
