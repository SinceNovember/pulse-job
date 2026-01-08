package com.simple.pulsejob.admin.common.model.enums;

import com.simple.plusejob.serialization.SerializerType;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 序列化类型枚举
 *
 * @author pulse
 */
@Getter
@AllArgsConstructor
public enum SerializerTypeEnum {

    /** Java 原生序列化 */
    JAVA("java", 1),

    /** Protostuff 序列化 */
    PROTO_STUFF("proto_stuff", 2),

    /** Hessian 序列化 */
    HESSIAN("hessian", 3),

    /** Kryo 序列化 */
    KRYO("kryo", 4),

    /** JSON 序列化 */
    JSON("json", 5);

    private final String name;
    private final Integer code;

    public static SerializerTypeEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (SerializerTypeEnum e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown SerializerTypeEnum code: " + code);
    }

    public static SerializerTypeEnum fromName(String name) {
        if (name == null) {
            return null;
        }
        for (SerializerTypeEnum e : values()) {
            if (e.name.equalsIgnoreCase(name)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Unknown SerializerTypeEnum name: " + name);
    }

    /**
     * 转换为 SerializerType（serialization 模块）
     */
    public SerializerType toSerializerType() {
        switch (this) {
            case JAVA:
                return SerializerType.JAVA;
            case PROTO_STUFF:
                return SerializerType.PROTO_STUFF;
            case HESSIAN:
                return SerializerType.HESSIAN;
            case KRYO:
                return SerializerType.KRYO;
            case JSON:
                // JSON 暂不支持，默认使用 JAVA
                return SerializerType.JAVA;
            default:
                return SerializerType.JAVA;
        }
    }

    /**
     * 从 SerializerType 转换
     */
    public static SerializerTypeEnum from(SerializerType type) {
        if (type == null) {
            return null;
        }
        switch (type) {
            case JAVA:
                return JAVA;
            case PROTO_STUFF:
                return PROTO_STUFF;
            case HESSIAN:
                return HESSIAN;
            case KRYO:
                return KRYO;
            default:
                return JAVA;
        }
    }
}

