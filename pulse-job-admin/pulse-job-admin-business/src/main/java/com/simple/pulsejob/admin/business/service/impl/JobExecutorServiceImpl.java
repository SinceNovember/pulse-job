package com.simple.pulsejob.admin.business.service.impl;

import com.simple.pulsejob.admin.business.service.IJobExecutorService;
import com.simple.pulsejob.admin.common.mapping.JobExecutorMapping;
import com.simple.pulsejob.admin.common.model.entity.JobExecutor;
import com.simple.pulsejob.admin.common.model.enums.RegisterTypeEnum;
import com.simple.pulsejob.admin.common.model.param.JobExecutorParam;
import com.simple.pulsejob.admin.persistence.mapper.JobExecutorMapper;
import com.simple.pulsejob.common.util.StringUtil;
import com.simple.pulsejob.common.util.Strings;
import com.simple.pulsejob.transport.channel.JChannel;
import com.simple.pulsejob.transport.metadata.ExecutorKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
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
    public void autoRegisterJobExecutor(JChannel channel, ExecutorKey executorWrapper) {
        log.info("auto register job executor channel : {}", channel);
        String executorName = executorWrapper.getExecutorName();
        String channelAddress = channel.remoteIpPort();

        JobExecutor jobExecutor = jobExecutorMapper.findByExecutorName(executorName)
            .map(existing -> existing.updateAddressIfAbsent(channelAddress))
            .orElseGet(() -> JobExecutor.of(executorName, channelAddress));

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

    @Override
    public void deregisterJobExecutor(String executorName, JChannel channel) {
        jobExecutorMapper.findByExecutorName(executorName)
            .ifPresent(jobExecutor -> {
                String ipPort = channel.remoteIpPort();
                String address = jobExecutor.getExecutorAddress();
                if (StringUtil.isBlank(address)) {
                    return;
                }

                List<String> addressList = Arrays.stream(address.split(Strings.SEMICOLON))
                    .map(String::trim)
                    .filter(addr -> !addr.equals(ipPort))
                    .collect(Collectors.toList());

                jobExecutor.setExecutorAddress(String.join(Strings.SEMICOLON, addressList));
                jobExecutorMapper.save(jobExecutor);
            });
    }

    @Override
    public void clearAllJobExecutorAddress() {
        jobExecutorMapper.updateAllExecutorAddressNull();
    }
} 