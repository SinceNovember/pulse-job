package com.simple.pulsejob.admin.scheduler.factory;

import com.simple.plusejob.serialization.Serializer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SerializerFactory {

    private final List<Serializer> serializers;

    private static Map<Byte, Serializer> SERIALIZER_MAP;

    public static Serializer getSerializer(Byte type) {
        Serializer serializer = SERIALIZER_MAP.get(type);
        if (serializer == null) {
            throw new IllegalArgumentException("Unsupported serializer type: " + type);
        }
        return serializer;
    }

    @PostConstruct
    public void buildSerializerFactory() {
        for (Serializer serializer : serializers) {
            SERIALIZER_MAP.put(serializer.code(), serializer);
        }
    }
}
