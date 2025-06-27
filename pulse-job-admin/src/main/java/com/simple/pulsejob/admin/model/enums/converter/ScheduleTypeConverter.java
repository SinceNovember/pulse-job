package com.simple.pulsejob.admin.model.enums.converter;

import com.simple.pulsejob.admin.model.enums.ScheduleTypeEnum;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ScheduleTypeConverter implements AttributeConverter<ScheduleTypeEnum, Integer> {

    @Override
    public Integer convertToDatabaseColumn(ScheduleTypeEnum status) {
        return status != null ? status.getCode() : null;
    }

    @Override
    public ScheduleTypeEnum convertToEntityAttribute(Integer dbData) {
        return dbData != null ? ScheduleTypeEnum.fromCode(dbData) : null;
    }
}