package com.simple.pulsejob.transport.payload;

import com.simple.plusejob.serialization.Serializer;
import com.simple.plusejob.serialization.SerializerType;
import com.simple.plusejob.serialization.io.OutputBuf;
import com.simple.pulsejob.transport.CodecConfig;
import com.simple.pulsejob.transport.channel.JChannel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Payload 序列化工具类.
 *
 * <p>集成序列化器管理，提供简洁的 API：</p>
 * <pre>{@code
 * // 1. 初始化时注册序列化器（通常在启动时由 SerializerHolder 自动完成）
 * PayloadSerializer.register(new JavaSerializer());
 *
 * // 2. 使用时只需指定类型，invokeId 自动生成
 * JRequestPayload payload = PayloadSerializer.request()
 *     .channel(channel)
 *     .type(SerializerType.JAVA)
 *     .message(data)
 *     .messageCode(JProtocolHeader.JOB_LOG_MESSAGE)
 *     .build();
 * }</pre>
 */
public final class PayloadSerializer {

    /** 序列化器注册表 */
    private static final Map<SerializerType, Serializer> SERIALIZERS = new ConcurrentHashMap<>();

    /** 默认序列化类型 */
    private static volatile SerializerType defaultType = SerializerType.JAVA;

    private PayloadSerializer() {}

    // ==================== 序列化器管理 ====================

    /**
     * 注册序列化器
     */
    public static void register(Serializer serializer) {
        if (serializer != null) {
            SERIALIZERS.put(serializer.code(), serializer);
        }
    }

    /**
     * 批量注册序列化器
     */
    public static void registerAll(Iterable<Serializer> serializers) {
        for (Serializer s : serializers) {
            register(s);
        }
    }

    /**
     * 获取序列化器
     */
    public static Serializer get(SerializerType type) {
        Serializer serializer = SERIALIZERS.get(type);
        if (serializer == null) {
            throw new IllegalStateException("Serializer not registered: " + type);
        }
        return serializer;
    }

    /**
     * 获取默认序列化器
     */
    public static Serializer getDefault() {
        return get(defaultType);
    }

    /**
     * 设置默认序列化类型
     */
    public static void setDefaultType(SerializerType type) {
        defaultType = type;
    }

    // ==================== 序列化方法 ====================

    /**
     * 序列化消息到 Payload
     */
    public static <T> void serialize(PayloadHolder payload,
                                     JChannel channel,
                                     Serializer serializer,
                                     T message,
                                     byte messageCode) {
        byte serializerCode = serializer.code().value();
        if (CodecConfig.isCodecLowCopy()) {
            OutputBuf outputBuf = serializer.writeObject(channel.allocOutputBuf(), message);
            payload.outputBuf(serializerCode, messageCode, outputBuf);
        } else {
            byte[] bytes = serializer.writeObject(message);
            payload.bytes(serializerCode, messageCode, bytes);
        }
    }

    /**
     * 序列化消息到 Payload（使用类型指定）
     */
    public static <T> void serialize(PayloadHolder payload,
                                     JChannel channel,
                                     SerializerType type,
                                     T message,
                                     byte messageCode) {
        serialize(payload, channel, get(type), message, messageCode);
    }

    /**
     * 序列化消息到 Payload（使用默认序列化器）
     */
    public static <T> void serialize(PayloadHolder payload,
                                     JChannel channel,
                                     T message,
                                     byte messageCode) {
        serialize(payload, channel, getDefault(), message, messageCode);
    }

    // ==================== 便捷创建方法 ====================

    /**
     * 创建 Request Payload（invokeId 自动生成）
     */
    public static <T> JRequestPayload createRequest(JChannel channel,
                                                    SerializerType type,
                                                    T message,
                                                    byte messageCode) {
        JRequestPayload payload = new JRequestPayload();
        serialize(payload, channel, type, message, messageCode);
        return payload;
    }

    /**
     * 创建 Request Payload（使用默认序列化器，invokeId 自动生成）
     */
    public static <T> JRequestPayload createRequest(JChannel channel,
                                                    T message,
                                                    byte messageCode) {
        return createRequest(channel, defaultType, message, messageCode);
    }

    /**
     * 创建 Response Payload
     */
    public static <T> JResponsePayload createResponse(long invokeId,
                                                      JChannel channel,
                                                      SerializerType type,
                                                      T message,
                                                      byte messageCode) {
        JResponsePayload payload = new JResponsePayload(invokeId);
        serialize(payload, channel, type, message, messageCode);
        return payload;
    }

    /**
     * 创建 Response Payload（使用默认序列化器）
     */
    public static <T> JResponsePayload createResponse(long invokeId,
                                                      JChannel channel,
                                                      T message,
                                                      byte messageCode) {
        return createResponse(invokeId, channel, defaultType, message, messageCode);
    }

    // ==================== 链式构建器 ====================

    /**
     * 创建 Request 构建器
     */
    public static RequestBuilder request() {
        return new RequestBuilder();
    }

    /**
     * 创建 Response 构建器
     */
    public static ResponseBuilder response() {
        return new ResponseBuilder();
    }

    /**
     * Request 构建器（invokeId 自动生成）
     */
    public static class RequestBuilder {
        private JChannel channel;
        private SerializerType type = defaultType;
        private Object message;
        private byte messageCode;

        public RequestBuilder channel(JChannel channel) {
            this.channel = channel;
            return this;
        }

        public RequestBuilder type(SerializerType type) {
            this.type = type;
            return this;
        }

        public RequestBuilder message(Object message) {
            this.message = message;
            return this;
        }

        public RequestBuilder messageCode(byte messageCode) {
            this.messageCode = messageCode;
            return this;
        }

        public JRequestPayload build() {
            return createRequest(channel, type, message, messageCode);
        }
    }

    /**
     * Response 构建器（需要指定 invokeId 以匹配请求）
     */
    public static class ResponseBuilder {
        private long invokeId;
        private JChannel channel;
        private SerializerType type = defaultType;
        private Object message;
        private byte messageCode;

        public ResponseBuilder invokeId(long invokeId) {
            this.invokeId = invokeId;
            return this;
        }

        public ResponseBuilder channel(JChannel channel) {
            this.channel = channel;
            return this;
        }

        public ResponseBuilder type(SerializerType type) {
            this.type = type;
            return this;
        }

        public ResponseBuilder message(Object message) {
            this.message = message;
            return this;
        }

        public ResponseBuilder messageCode(byte messageCode) {
            this.messageCode = messageCode;
            return this;
        }

        public JResponsePayload build() {
            return createResponse(invokeId, channel, type, message, messageCode);
        }
    }
}
