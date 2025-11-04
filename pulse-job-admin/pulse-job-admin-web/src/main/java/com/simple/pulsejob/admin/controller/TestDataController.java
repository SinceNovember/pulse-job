package com.simple.pulsejob.admin.controller;

import com.simple.pulsejob.admin.business.service.IJobInfoService;
import com.simple.pulsejob.admin.common.model.base.ResponseResult;
import com.simple.pulsejob.admin.common.model.entity.JobInfo;
import com.simple.pulsejob.admin.common.model.enums.ScheduleTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestDataController {

    private final IJobInfoService jobInfoService;

    /**
     * 插入测试数据
     */
    @PostMapping("/insert-test-data")
    public ResponseResult<Void> insertTestData() {
        try {
            List<JobInfo> testJobs = new ArrayList<>();
            
            // 创建测试任务1：5秒后执行
            JobInfo job1 = new JobInfo();
            job1.setJobHandler("testJob1");
            job1.setScheduleRate("0/30 * * * * ?"); // 每30秒执行一次
            job1.setScheduleType(ScheduleTypeEnum.CRON);
            job1.setExecutorId(1);
            job1.setStatus(1);
            job1.setNextExecuteTime(LocalDateTime.now().plusSeconds(5));
            job1.setDescription("测试任务1");
            job1.setCreateTime(LocalDateTime.now());
            job1.setUpdateTime(LocalDateTime.now());
            testJobs.add(job1);
            
            // 创建测试任务2：10秒后执行
            JobInfo job2 = new JobInfo();
            job2.setJobHandler("testJob2");
            job2.setScheduleRate("0 0/1 * * * ?"); // 每分钟执行一次
            job2.setScheduleType(ScheduleTypeEnum.CRON);
            job2.setExecutorId(1);
            job2.setStatus(1);
            job2.setNextExecuteTime(LocalDateTime.now().plusSeconds(10));
            job2.setDescription("测试任务2");
            job2.setCreateTime(LocalDateTime.now());
            job2.setUpdateTime(LocalDateTime.now());
            testJobs.add(job2);
            
            // 创建测试任务3：15秒后执行
            JobInfo job3 = new JobInfo();
            job3.setJobHandler("testJob3");
            job3.setScheduleRate("0 0 0/1 * * ?"); // 每小时执行一次
            job3.setScheduleType(ScheduleTypeEnum.CRON);
            job3.setExecutorId(1);
            job3.setStatus(1);
            job3.setNextExecuteTime(LocalDateTime.now().plusSeconds(15));
            job3.setDescription("测试任务3");
            job3.setCreateTime(LocalDateTime.now());
            job3.setUpdateTime(LocalDateTime.now());
            testJobs.add(job3);
            
            // 保存测试数据
            jobInfoService.batchAddJobInfos(null); // 这里需要转换，暂时直接保存
            for (JobInfo job : testJobs) {
                jobInfoService.updateJobInfo(job);
            }
            
            log.info("成功插入 {} 个测试任务", testJobs.size());
            return ResponseResult.ok();
            
        } catch (Exception e) {
            log.error("插入测试数据失败", e);
            return ResponseResult.error("插入测试数据失败: " + e.getMessage());
        }
    }
} 