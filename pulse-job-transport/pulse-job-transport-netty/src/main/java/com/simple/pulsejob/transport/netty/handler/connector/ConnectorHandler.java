package com.simple.pulsejob.transport.netty.handler.connector;

import java.io.IOException;
import com.simple.pulsejob.common.util.Signal;
import com.simple.pulsejob.common.util.StackTraceUtil;
import com.simple.pulsejob.common.util.internal.logging.InternalLogger;
import com.simple.pulsejob.common.util.internal.logging.InternalLoggerFactory;
import com.simple.pulsejob.transport.Status;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.netty.channel.NettyChannel;
import com.simple.pulsejob.transport.payload.JRequestPayload;
import com.simple.pulsejob.transport.payload.JResponsePayload;
import com.simple.pulsejob.transport.processor.AcceptorProcessor;
import com.simple.pulsejob.transport.processor.ConnectorProcessor;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.DecoderException;
import io.netty.util.ReferenceCountUtil;
import lombok.Value;


@ChannelHandler.Sharable
public class ConnectorHandler extends ChannelInboundHandlerAdapter {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ConnectorHandler.class);

    private ConnectorProcessor processor;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        processor.handleActive(NettyChannel.attachChannel(ctx.channel()));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        processor.handleInactive(NettyChannel.attachChannel(ctx.channel()));
        ctx.fireChannelInactive();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel ch = ctx.channel();

        if (msg instanceof JRequestPayload) {
            JChannel jChannel = NettyChannel.attachChannel(ch);
            try {
                processor.handleRequest(jChannel, (JRequestPayload) msg);
            } catch (Throwable t) {
                processor.handleException(jChannel, (JRequestPayload) msg, Status.SERVER_ERROR, t);
            }
        } else {
            logger.warn("Unexpected message type received: {}, channel: {}.", msg.getClass(), ch);

            ReferenceCountUtil.release(msg);
        }
    }


    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        Channel ch = ctx.channel();
        ChannelConfig config = ch.config();

        // 高水位线: ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK
        // 低水位线: ChannelOption.WRITE_BUFFER_LOW_WATER_MARK
        if (!ch.isWritable()) {
            // 当前channel的缓冲区(OutboundBuffer)大小超过了WRITE_BUFFER_HIGH_WATER_MARK
            if (logger.isWarnEnabled()) {
                logger.warn("{} is not writable, high water mask: {}, the number of flushed entries that are not written yet: {}.",
                    ch, config.getWriteBufferHighWaterMark(), ch.unsafe().outboundBuffer().size());
            }

            config.setAutoRead(false);
        } else {
            // 曾经高于高水位线的OutboundBuffer现在已经低于WRITE_BUFFER_LOW_WATER_MARK了
            if (logger.isWarnEnabled()) {
                logger.warn("{} is writable(rehabilitate), low water mask: {}, the number of flushed entries that are not written yet: {}.",
                    ch, config.getWriteBufferLowWaterMark(), ch.unsafe().outboundBuffer().size());
            }

            config.setAutoRead(true);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Channel ch = ctx.channel();

        if (cause instanceof Signal) {
            logger.error("I/O signal was caught: {}, force to close channel: {}.", ((Signal) cause).name(), ch);

            ch.close();
        } else if (cause instanceof IOException) {
            logger.error("I/O exception was caught: {}, force to close channel: {}.", StackTraceUtil.stackTrace(cause), ch);

            ch.close();
        } else if (cause instanceof DecoderException) {
            logger.error("Decoder exception was caught: {}, force to close channel: {}.", StackTraceUtil.stackTrace(cause), ch);

            ch.close();
        } else {
            logger.error("Unexpected exception was caught: {}, channel: {}.", StackTraceUtil.stackTrace(cause), ch);
        }
    }

    public ConnectorProcessor processor() {
        return processor;
    }

    public void processor(ConnectorProcessor processor) {
        this.processor = processor;
    }
}
