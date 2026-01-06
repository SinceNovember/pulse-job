package com.simple.pulsejob.transport.netty.handler;

import com.simple.pulsejob.common.util.Reflects;
import com.simple.pulsejob.transport.JProtocolHeader;
import com.simple.pulsejob.transport.payload.JRequestPayload;
import com.simple.pulsejob.transport.payload.JResponsePayload;
import com.simple.pulsejob.transport.payload.PayloadHolder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * <pre>
 * **************************************************************************************************
 *                                          Protocol
 *  ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐
 *       2   │   1   │    1   │     8     │      4      │
 *  ├ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤
 *           │       │        │           │             │
 *  │  MAGIC   Sign    Status   Invoke Id    Body Size                    Body Content              │
 *           │       │        │           │             │
 *  └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘
 *
 * 消息头16个字节定长
 * = 2 // magic = (short) 0xbabe
 * + 1 // 消息标志位, 低地址4位用来表示消息类型request/response/heartbeat等, 高地址4位用来表示序列化类型
 * + 1 // 状态位, 设置请求响应状态
 * + 8 // 消息 instanceId, long 类型, 未来jupiter可能将id限制在48位, 留出高地址的16位作为扩展字段
 * + 4 // 消息体 body 长度, int 类型
 * </pre>
 * <p>
 * jupiter
 * org.jupiter.transport.netty.handler
 *
 * @author jiachun.fjc
 */
@ChannelHandler.Sharable
public class ProtocolEncoder extends MessageToByteEncoder<PayloadHolder> {

    @Override
    protected void encode(ChannelHandlerContext ctx, PayloadHolder msg, ByteBuf out) throws Exception {
        if (msg instanceof JRequestPayload) {
            doEncodeRequest((JRequestPayload) msg, out);
        } else if (msg instanceof JResponsePayload) {
            doEncodeResponse((JResponsePayload) msg, out);
        } else {
            throw new IllegalArgumentException(Reflects.simpleClassName(msg));
        }
    }

    @Override
    protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, PayloadHolder msg, boolean preferDirect) throws Exception {
        if (preferDirect) {
            return ctx.alloc().ioBuffer(JProtocolHeader.HEADER_SIZE + msg.size());
        } else {
            return ctx.alloc().heapBuffer(JProtocolHeader.HEADER_SIZE + msg.size());
        }
    }

    private void doEncodeRequest(JRequestPayload request, ByteBuf out) {
        byte sign = JProtocolHeader.toSign(request.serializerCode(), request.messageCode());
        long instanceId = request.instanceId();
        byte[] bytes = request.bytes();
        int length = bytes.length;

        out.writeShort(JProtocolHeader.MAGIC)
            .writeByte(sign)
            .writeByte(0x00)
            .writeLong(instanceId)
            .writeInt(length)
            .writeBytes(bytes);
    }

    private void doEncodeResponse(JResponsePayload response, ByteBuf out) {
        byte sign = JProtocolHeader.toSign(response.serializerCode(), response.messageCode());
        byte status = response.status();
        long instanceId = response.instanceId();
        byte[] bytes = response.bytes();
        int length = bytes.length;

        out.writeShort(JProtocolHeader.MAGIC)
            .writeByte(sign)
            .writeByte(status)
            .writeLong(instanceId)
            .writeInt(length)
            .writeBytes(bytes);
    }
}
