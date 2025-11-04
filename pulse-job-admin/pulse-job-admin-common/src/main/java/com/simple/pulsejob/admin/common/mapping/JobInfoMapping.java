package com.simple.pulsejob.admin.common.mapping;

import java.util.List;
import com.simple.pulsejob.admin.common.model.entity.JobInfo;
import com.simple.pulsejob.admin.common.model.param.JobInfoParam;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface JobInfoMapping {

    JobInfoMapping INSTANCE = Mappers.getMapper(JobInfoMapping.class);

    JobInfo toJobInfo(JobInfoParam param);

    List<JobInfo> toJobInfoList(List<JobInfoParam> params);


}
