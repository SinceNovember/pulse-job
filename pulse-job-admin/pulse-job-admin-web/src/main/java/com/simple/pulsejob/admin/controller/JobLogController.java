package com.simple.pulsejob.admin.controller;

import com.simple.pulsejob.admin.business.service.IJobLogService;
import com.simple.pulsejob.admin.common.model.base.ResponseResult;
import com.simple.pulsejob.admin.common.model.entity.JobLog;
import com.simple.pulsejob.admin.common.model.enums.LogLevelEnum;
import com.simple.pulsejob.admin.scheduler.log.JobLogStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务日志接口.
 * 
 * <p>提供日志查询、统计、清理等功能</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/job-log")
@RequiredArgsConstructor
public class JobLogController {

    private final IJobLogService jobLogService;
    private final JobLogStorageService jobLogStorageService;

}

