package com.simple.pulsejob.admin.mapping;

import java.util.List;
import com.simple.pulsejob.admin.model.entity.JobExecutor;
import com.simple.pulsejob.admin.model.param.JobExecutorParam;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface JobExecutorMapping {

    JobExecutorMapping INSTANCE = Mappers.getMapper(JobExecutorMapping.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "updateTime", expression = "java(java.time.LocalDateTime.now())")
    JobExecutor toJobExecutor(JobExecutorParam param);

    List<JobExecutor> toJobExecutorList(List<JobExecutorParam> params);
} 