package com.simple.pulsejob.admin.scheduler.factory;

import com.simple.plusejob.serialization.Serializer;
import com.simple.plusejob.serialization.SerializerType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class SerializerFactory extends EnumBeanFactory<SerializerType, Serializer>{

    public SerializerFactory(List<Serializer> serializers) {
        super(serializers, Serializer::code, "Duplicate Serializer type detected: ");
    }

}