package com.simple.pulsejob.transport;

/**
 * Jupiter传输层协议头
 *
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
 *
 * jupiter
 * org.jupiter.transport
 *
 * @author jiachun.fjc
 */
public class JProtocolHeader {

    /** 协议头长度 */
    public static final int HEADER_SIZE = 16;
    /** Magic */
    public static final short MAGIC = (short) 0xbabe;


    /** Message Code: 0x01 ~ 0x0f =================================================================================== */
    public static final byte REQUEST                    = 0x01;     // Request
    public static final byte RESPONSE                   = 0x02;     // Response
    public static final byte REGISTER_EXECUTOR          = 0x03;     // 注册执行器
    public static final byte ACK                        = 0x04;     // Acknowledge
    public static final byte TRIGGER_JOB                = 0x05;     // 触发任务执行
    public static final byte JOB_LOG_MESSAGE            = 0x06;     // 任务执行日志（流式）
    public static final byte JOB_RESULT                 = 0x07;     // 任务执行结果
    public static final byte HEARTBEAT                  = 0x08;     // Heartbeat

    private byte messageCode; // sign 低地址4位

    /**
     * Serializer Code: 0x01 ~ 0x0f ================================================================================
     */
    // 位数限制最多支持15种不同的序列化/反序列化方式
    // protostuff   = 0x01
    // hessian      = 0x02
    // kryo         = 0x03
    // java         = 0x04
    // ...
    // XX1          = 0x0e
    // XX2          = 0x0f
    private byte serializerCode;
    private byte status;
    private long id;
    private int bodySize;

    public static byte toSign(byte serializerCode, byte messageCode) {
        return (byte) ((serializerCode << 4) | (messageCode & 0x0f));
    }

    public void sign(byte sign) {
        this.messageCode = (byte) (sign & 0x0f);
        this.serializerCode = (byte) ((((int) sign) & 0xff) >> 4);
    }


    public byte messageCode() {
        return messageCode;
    }

    public byte serializerCode() {
        return serializerCode;
    }

    public byte status() {
        return status;
    }

    public void status(byte status) {
        this.status = status;
    }

    public long id() {
        return id;
    }

    public void id(long id) {
        this.id = id;
    }

    public int bodySize() {
        return bodySize;
    }

    public void bodySize(int bodyLength) {
        this.bodySize = bodyLength;
    }

    @Override
    public String toString() {
        return "JProtocolHeader{" +
            "messageCode=" + messageCode +
            ", serializerCode=" + serializerCode +
            ", status=" + status +
            ", id=" + id +
            ", bodySize=" + bodySize +
            '}';
    }
}
