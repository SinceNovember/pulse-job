package com.simple.pulsejob.admin.scheduler.factory;

import com.simple.plusejob.serialization.Serializer;
import com.simple.plusejob.serialization.SerializerType;
import com.simple.pulsejob.admin.common.model.enums.SerializerTypeEnum;
import com.simple.pulsejob.transport.payload.PayloadSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class SerializerFactory extends EnumBeanFactory<SerializerType, Serializer> {

    public SerializerFactory(List<Serializer> serializers) {
        super(serializers, Serializer::code, "Duplicate Serializer type detected: ");
        // 统一注册到 PayloadSerializer
        PayloadSerializer.registerAll(serializers);
        log.info("已注册 {} 个序列化器到 PayloadSerializer", serializers.size());
    }

    /**
     * 根据 SerializerTypeEnum 获取序列化器
     */
    public Serializer get(SerializerTypeEnum type) {
        if (type == null) {
            return get(SerializerType.JAVA);
        }
        return get(type.toSerializerType());
    }
}