package com.simple.pulsejob.transport.netty.handler;

import com.simple.pulsejob.transport.Acknowledge;
import com.simple.pulsejob.transport.JProtocolHeader;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

@ChannelHandler.Sharable
public class AcknowledgeEncoder extends MessageToByteEncoder<Acknowledge> {
    @Override
    protected void encode(ChannelHandlerContext ctx, Acknowledge ack, ByteBuf out) throws Exception {
        out.writeShort(JProtocolHeader.MAGIC)
            .writeByte(JProtocolHeader.ACK)
            .writeByte(0)
            .writeLong(ack.sequence())
            .writeInt(0);

    }
}
