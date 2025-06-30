package com.simple.pulsejob.admin.service.impl;

import com.simple.pulsejob.admin.mapping.JobExecutorMapping;
import com.simple.pulsejob.admin.model.entity.JobExecutor;
import com.simple.pulsejob.admin.mapper.JobExecutorMapper;
import com.simple.pulsejob.admin.model.enums.RegisterTypeEnum;
import com.simple.pulsejob.admin.model.param.JobExecutorParam;
import com.simple.pulsejob.admin.service.IJobExecutorService;
import com.simple.pulsejob.common.util.StringUtil;
import com.simple.pulsejob.common.util.Strings;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.metadata.JobExecutorWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

@Service
@RequiredArgsConstructor
public class JobExecutorServiceImpl implements IJobExecutorService {

    private final JobExecutorMapper jobExecutorMapper;

    @Override
    public void addJobExecutor(JobExecutorParam jobExecutorParam) {
        JobExecutor jobExecutor = JobExecutorMapping.INSTANCE.toJobExecutor(jobExecutorParam);
        jobExecutorMapper.save(jobExecutor);
    }

    @Override
    public void autoRegisterJobExecutor(JChannel channel, JobExecutorWrapper executorWrapper) {
        String executorName = executorWrapper.getExecutorName();
        String newAddress = channel.remoteIpPort();

        JobExecutor jobExecutor = jobExecutorMapper.findByExecutorName(executorName)
            .map(existing -> existing.updateAddressIfAbsent(newAddress))
            .orElseGet(() -> JobExecutor.of(executorName, channel.remoteIp()));

        jobExecutor.refreshUpdateTime();
        jobExecutorMapper.save(jobExecutor);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<JobExecutor> getJobExecutorById(Integer id) {
        return jobExecutorMapper.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<JobExecutor> getJobExecutorByName(String executorName) {
        return jobExecutorMapper.findByExecutorName(executorName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobExecutor> getAllJobExecutors() {
        return jobExecutorMapper.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobExecutor> getJobExecutorsByRegisterType(RegisterTypeEnum registerType) {
        return jobExecutorMapper.findByRegisterType(registerType);
    }

    @Override
    public JobExecutor updateJobExecutor(JobExecutor jobExecutor) {
        if (jobExecutor.getId() == null) {
            throw new IllegalArgumentException("更新执行器信息时ID不能为空");
        }
        jobExecutor.setUpdateTime(LocalDateTime.now());
        return jobExecutorMapper.save(jobExecutor);
    }

    @Override
    public void deleteJobExecutor(Integer id) {
        jobExecutorMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAddJobExecutors(List<JobExecutorParam> jobExecutorParams) {
        if (jobExecutorParams == null || jobExecutorParams.isEmpty()) {
            throw new IllegalArgumentException("批量保存的执行器信息列表不能为空");
        }
        List<JobExecutor> jobExecutors = JobExecutorMapping.INSTANCE.toJobExecutorList(jobExecutorParams);
        jobExecutorMapper.saveAll(jobExecutors);
    }
} 