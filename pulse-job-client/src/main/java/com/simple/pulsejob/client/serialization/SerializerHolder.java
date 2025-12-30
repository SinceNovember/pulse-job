package com.simple.pulsejob.client.serialization;

import com.simple.plusejob.serialization.Serializer;
import com.simple.plusejob.serialization.SerializerType;
import com.simple.pulsejob.transport.payload.PayloadSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 序列化器持有者 - 统一管理所有序列化器.
 *
 * <p>使用方式：</p>
 * <pre>{@code
 * @Autowired
 * private SerializerHolder serializerHolder;
 *
 * // 获取序列化器
 * Serializer serializer = serializerHolder.get(SerializerType.JAVA);
 * Serializer serializer = serializerHolder.getByCode((byte) 0x04);
 * Serializer serializer = serializerHolder.getDefault();
 * }</pre>
 */
@Slf4j
@Component
public class SerializerHolder {

    private final Map<SerializerType, Serializer> typeMap;
    private final Map<Byte, Serializer> codeMap;
    private final Serializer defaultSerializer;

    public SerializerHolder(List<Serializer> serializers) {
        // 按 SerializerType 索引
        this.typeMap = serializers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        Serializer::code,
                        Function.identity(),
                        (a, b) -> { throw new IllegalStateException("Duplicate serializer: " + a.code()); }
                ));

        this.defaultSerializer = typeMap.getOrDefault(SerializerType.JAVA,
                serializers.isEmpty() ? null : serializers.get(0));

        this.codeMap = serializers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        s -> s.code().value(),
                        Function.identity()));

        // 自动注册到 PayloadSerializer
        PayloadSerializer.registerAll(serializers);
        log.info("已注册 {} 个序列化器到 PayloadSerializer: {}",
                serializers.size(), typeMap.keySet());
    }

    /**
     * 按类型获取序列化器
     */
    public Serializer get(SerializerType type) {
        Serializer serializer = typeMap.get(type);
        if (serializer == null) {
            throw new IllegalArgumentException("Serializer not found: " + type);
        }
        return serializer;
    }

    /**
     * 按字节码获取序列化器
     */
    public Serializer getByCode(byte code) {
        Serializer serializer = codeMap.get(code);
        if (serializer == null) {
            throw new IllegalArgumentException("Serializer not found for code: " + code);
        }
        return serializer;
    }


    /**
     * 获取默认序列化器
     */
    public Serializer getDefault() {
        if (defaultSerializer == null) {
            throw new IllegalStateException("No serializer available");
        }
        return defaultSerializer;
    }

    /**
     * 检查是否支持某类型
     */
    public boolean supports(SerializerType type) {
        return typeMap.containsKey(type);
    }

    /**
     * 获取所有支持的类型
     */
    public Set<SerializerType> supportedTypes() {
        return typeMap.keySet();
    }
}

