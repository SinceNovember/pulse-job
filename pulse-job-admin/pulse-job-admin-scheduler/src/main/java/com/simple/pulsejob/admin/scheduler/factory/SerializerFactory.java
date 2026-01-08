package com.simple.pulsejob.admin.scheduler.factory;

import com.simple.plusejob.serialization.Serializer;
import com.simple.plusejob.serialization.SerializerType;
import com.simple.pulsejob.admin.common.model.enums.SerializerTypeEnum;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SerializerFactory extends EnumBeanFactory<SerializerType, Serializer> {

    public SerializerFactory(List<Serializer> serializers) {
        super(serializers, Serializer::code, "Duplicate Serializer type detected: ");
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