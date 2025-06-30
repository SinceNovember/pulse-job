package com.simple.pulsejob.admin.mapping;

import java.util.List;
import com.simple.pulsejob.admin.model.entity.JobInfo;
import com.simple.pulsejob.admin.model.param.JobInfoParam;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface JobInfoMapping {

    JobInfoMapping INSTANCE = Mappers.getMapper(JobInfoMapping.class);

    JobInfo toJobInfo(JobInfoParam param);

    List<JobInfo> toJobInfoList(List<JobInfoParam> params);


}
