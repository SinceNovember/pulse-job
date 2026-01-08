package com.simple.pulsejob.admin.common.model.dto;

import com.simple.pulsejob.admin.common.model.entity.JobInfo;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JobInfoWithExecutorDTO {

    private JobInfo jobInfo;

    private String executorName;

}

