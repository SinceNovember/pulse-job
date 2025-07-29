package com.simple.pulsejob.transport.netty;

import java.util.function.Consumer;
import com.simple.pulsejob.transport.JConnection;
import com.simple.pulsejob.transport.UnresolvedAddress;
import com.simple.pulsejob.transport.channel.JChannel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

public abstract class JNettyConnection extends JConnection {
    private final ChannelFuture future;

    private final Consumer<JChannel> connectionCallback;

    public JNettyConnection(UnresolvedAddress address, ChannelFuture future, Consumer<JChannel> connectionCallback) {
        super(address);
        this.future = future;
        this.connectionCallback = connectionCallback;
    }

    public JNettyConnection(UnresolvedAddress address, ChannelFuture future) {
        super(address);
        this.future = future;
        this.connectionCallback = null;
    }

    public ChannelFuture getFuture() {
        return future;
    }

    public Consumer<JChannel> getConnectionCallback() {
        return connectionCallback;
    }

    @Override
    public void operationComplete(final OperationListener operationListener) {
        future.addListener((ChannelFutureListener) future -> operationListener.complete(future.isSuccess()));
    }
}
