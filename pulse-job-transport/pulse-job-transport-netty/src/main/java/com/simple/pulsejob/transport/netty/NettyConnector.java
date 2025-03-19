package com.simple.pulsejob.transport.netty;

import java.util.concurrent.ConcurrentMap;
import com.simple.pulsejob.common.JConstants;
import com.simple.pulsejob.common.concurrent.JNamedThreadFactory;
import com.simple.pulsejob.common.util.ClassUtil;
import com.simple.pulsejob.common.util.Maps;
import com.simple.pulsejob.transport.JConnection;
import com.simple.pulsejob.transport.JConnectionManager;
import com.simple.pulsejob.transport.JConnector;
import com.simple.pulsejob.transport.UnresolvedAddress;
import com.simple.pulsejob.transport.channel.JChannelGroup;
import com.simple.pulsejob.transport.processor.ConsumerProcessor;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
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



}
