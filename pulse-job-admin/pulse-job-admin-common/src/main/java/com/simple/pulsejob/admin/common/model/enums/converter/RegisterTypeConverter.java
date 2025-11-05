package com.simple.pulsejob.admin.common.model.enums.converter;

import com.simple.pulsejob.admin.common.model.enums.RegisterTypeEnum;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RegisterTypeConverter implements AttributeConverter<RegisterTypeEnum, Integer> {

    @Override
    public Integer convertToDatabaseColumn(RegisterTypeEnum attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getCode();
    }

    @Override
    public RegisterTypeEnum convertToEntityAttribute(Integer dbData) {
        return dbData != null ? RegisterTypeEnum.fromCode(dbData) : null;
    }
}