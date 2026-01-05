package com.simple.pulsejob.transport.netty.handler;

import com.simple.pulsejob.common.util.Signal;
import com.simple.pulsejob.common.util.SystemClock;
import com.simple.pulsejob.common.util.SystemPropertyUtil;
import com.simple.pulsejob.transport.JProtocolHeader;
import com.simple.pulsejob.transport.exception.IoSignals;
import com.simple.pulsejob.transport.payload.JRequestPayload;
import com.simple.pulsejob.transport.payload.JResponsePayload;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

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
 * + 8 // 消息 id, long 类型, 未来jupiter可能将id限制在48位, 留出高地址的16位作为扩展字段
 * + 4 // 消息体 body 长度, int 类型
 * </pre>
 *
 * jupiter
 * org.jupiter.transport.netty.handler
 *
 * @author jiachun.fjc
 */
public class ProtocolDecoder extends ReplayingDecoder<ProtocolDecoder.State> {

    // 协议体最大限制, 默认5M
    private static final int MAX_BODY_SIZE = SystemPropertyUtil.getInt("jupiter.io.decoder.max.body.size", 1024 * 1024 * 5);

    private static final boolean USE_COMPOSITE_BUF = SystemPropertyUtil.getBoolean("jupiter.io.decoder.composite.buf", false);

    public ProtocolDecoder() {
        super(State.MAGIC);
        if (USE_COMPOSITE_BUF) {
            setCumulator(COMPOSITE_CUMULATOR);
        }
    }

    // 协议头
    private final JProtocolHeader header = new JProtocolHeader();

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        switch (state()) {
            case MAGIC:
                checkMagic(in.readShort());         // MAGIC
                checkpoint(State.SIGN);
            case SIGN:
                header.sign(in.readByte());         // 消息标志位
                checkpoint(State.STATUS);
            case STATUS:
                header.status(in.readByte());       // 状态位
                checkpoint(State.ID);
            case ID:
                header.id(in.readLong());           // 消息id
                checkpoint(State.BODY_SIZE);
            case BODY_SIZE:
                header.bodySize(in.readInt());      // 消息体长度
                checkpoint(State.BODY);
            case BODY:
                switch (header.messageCode()) {
                    case JProtocolHeader.HEARTBEAT:
                        break;
                    // 请求类消息 -> JRequestPayload
                    case JProtocolHeader.JOB_LOG_MESSAGE:
                    case JProtocolHeader.JOB_BATCH_LOG_MESSAGE:
                    case JProtocolHeader.REGISTER_EXECUTOR:
                    case JProtocolHeader.TRIGGER_JOB:
                    case JProtocolHeader.JOB_RESULT:
                    case JProtocolHeader.REQUEST: {
                        int length = checkBodySize(header.bodySize());
                        byte[] bytes = new byte[length];
                        in.readBytes(bytes);

                        JRequestPayload request = new JRequestPayload(header.id());
                        request.timestamp(SystemClock.millisClock().now());
                        request.bytes(header.serializerCode(), header.messageCode(), bytes);

                        out.add(request);
                        break;
                    }
                    // 响应类消息 -> JResponsePayload
                    case JProtocolHeader.RESPONSE: {
                        int length = checkBodySize(header.bodySize());
                        byte[] bytes = new byte[length];
                        in.readBytes(bytes);

                        JResponsePayload response = new JResponsePayload(header.id());
                        response.status(header.status());
                        response.bytes(header.serializerCode(), header.messageCode(), bytes);

                        out.add(response);
                        break;
                    }

                    default:
                        throw IoSignals.ILLEGAL_SIGN;
                }
                checkpoint(State.MAGIC);
        }
    }

    private static void checkMagic(short magic) throws Signal {
        if (magic != JProtocolHeader.MAGIC) {
            throw IoSignals.ILLEGAL_MAGIC;
        }
    }

    private static int checkBodySize(int size) throws Signal {
        if (size > MAX_BODY_SIZE) {
            throw IoSignals.BODY_TOO_LARGE;
        }
        return size;
    }

    enum State {
        MAGIC,
        SIGN,
        STATUS,
        ID,
        BODY_SIZE,
        BODY
    }
}
