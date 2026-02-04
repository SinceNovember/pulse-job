package com.simple.pulsejob.admin.business.service.impl;

import com.simple.pulsejob.admin.business.service.IJobInfoService;
import com.simple.pulsejob.admin.common.mapping.JobInfoMapping;
import com.simple.pulsejob.admin.common.model.base.PageResult;
import com.simple.pulsejob.admin.common.model.dto.JobInfoWithExecutorDTO;
import com.simple.pulsejob.admin.common.model.entity.JobInfo;
import com.simple.pulsejob.admin.common.model.param.JobInfoParam;
import com.simple.pulsejob.admin.common.model.param.JobInfoQuery;
import com.simple.pulsejob.admin.persistence.mapper.JobInfoMapper;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobInfoServiceImpl implements IJobInfoService {

    private final JobInfoMapper jobInfoMapper;

    @Override
    public void addJobInfo(JobInfoParam jobInfoParam) {
        JobInfo jobInfo = JobInfoMapping.INSTANCE.toJobInfo(jobInfoParam);
        jobInfo.setCreateTime(LocalDateTime.now());
        jobInfo.setUpdateTime(LocalDateTime.now());
        jobInfoMapper.save(jobInfo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<JobInfo> getJobInfoById(Integer id) {
        return jobInfoMapper.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobInfo> getAllJobInfos() {
        return jobInfoMapper.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobInfo> getJobInfosByHandler(String jobHandler) {
        return jobInfoMapper.findByJobHandler(jobHandler);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobInfo> getJobInfosByExecutorId(Integer executorId) {
        return jobInfoMapper.findByExecutorId(executorId);
    }

    @Override
    public JobInfo updateJobInfo(JobInfo jobInfo) {
        if (jobInfo.getId() == null) {
            throw new IllegalArgumentException("更新任务信息时ID不能为空");
        }
        jobInfo.setUpdateTime(LocalDateTime.now());
        return jobInfoMapper.save(jobInfo);
    }

    @Override
    public void deleteJobInfo(Integer id) {
        jobInfoMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAddJobInfos(List<JobInfoParam> jobInfoParams) {
        if (jobInfoParams == null || jobInfoParams.isEmpty()) {
            throw new IllegalArgumentException("批量保存的任务信息列表不能为空");
        }
        List<JobInfo> jobInfos = JobInfoMapping.INSTANCE.toJobInfoList(jobInfoParams);
        jobInfos.forEach(jobInfo -> {
            jobInfo.setCreateTime(LocalDateTime.now());
            jobInfo.setUpdateTime(LocalDateTime.now());
        });
        jobInfoMapper.saveAll(jobInfos);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobInfo> findJobsToExecute(LocalDateTime startTime, LocalDateTime endTime) {
        return jobInfoMapper.findJobsToExecute(startTime, endTime, 1);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobInfo> findEnabledJobs() {
        return jobInfoMapper.findByStatus(1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNextExecuteTime(Integer jobId, LocalDateTime nextExecuteTime) {
        jobInfoMapper.updateNextExecuteTime(jobId, nextExecuteTime, LocalDateTime.now());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateExecutionStatus(Integer jobId, LocalDateTime lastExecuteTime, Integer retryTimes) {
        jobInfoMapper.updateExecutionStatus(jobId, lastExecuteTime, retryTimes, LocalDateTime.now());
    }

    @Override
    public Optional<JobInfoWithExecutorDTO> getJobInfoWithExecutorNameById(Integer jobId) {
        return jobInfoMapper.findWithExecutorNameById(jobId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<JobInfo> pageJobInfos(JobInfoQuery query) {
        // 构建排序
        Sort sort = Sort.by(Sort.Direction.DESC, "updateTime");
        if (StringUtils.hasText(query.getSortField())) {
            Sort.Direction direction = "asc".equalsIgnoreCase(query.getSortOrder()) 
                    ? Sort.Direction.ASC : Sort.Direction.DESC;
            sort = Sort.by(direction, query.getSortField());
        }

        // 构建分页
        Pageable pageable = PageRequest.of(query.getPageIndex(), query.getPageSize(), sort);

        // 构建查询条件
        Specification<JobInfo> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 任务处理器名称模糊匹配
            if (StringUtils.hasText(query.getJobHandler())) {
                predicates.add(cb.like(root.get("jobHandler"), "%" + query.getJobHandler() + "%"));
            }

            // 任务描述模糊匹配
            if (StringUtils.hasText(query.getDescription())) {
                predicates.add(cb.like(root.get("description"), "%" + query.getDescription() + "%"));
            }

            // 执行器ID精确匹配
            if (query.getExecutorId() != null) {
                predicates.add(cb.equal(root.get("executorId"), query.getExecutorId()));
            }

            // 调度类型精确匹配
            if (query.getScheduleType() != null) {
                predicates.add(cb.equal(root.get("scheduleType"), query.getScheduleType()));
            }

            // 状态精确匹配
            if (query.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), query.getStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<JobInfo> page = jobInfoMapper.findAll(spec, pageable);
        return PageResult.of(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setJobEnabled(Integer id, boolean enabled) {
        jobInfoMapper.findById(id).ifPresent(job -> {
            job.setStatus(enabled ? 1 : 0);
            job.setUpdateTime(LocalDateTime.now());
            jobInfoMapper.save(job);
            log.info("任务状态已更新: id={}, enabled={}", id, enabled);
        });
    }
}    