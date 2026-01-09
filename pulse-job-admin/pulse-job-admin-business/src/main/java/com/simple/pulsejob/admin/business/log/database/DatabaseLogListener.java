package com.simple.pulsejob.admin.business.log.database;

import com.simple.pulsejob.admin.common.model.entity.JobLog;
import com.simple.pulsejob.admin.common.model.enums.LogLevelEnum;
import com.simple.pulsejob.admin.persistence.mapper.JobLogMapper;
import com.simple.pulsejob.admin.scheduler.log.JobLogListener;
import com.simple.pulsejob.transport.metadata.LogMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据库日志写入监听器.
 *
 * <p>将任务执行日志持久化到数据库。</p>
 *
 * <p>配置项：</p>
 * <pre>
 * pulse-job.log.listener.database.enabled=true  # 启用数据库存储（默认 true）
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "pulse-job.log.listener.database.enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseLogListener implements JobLogListener {

    private final JobLogMapper jobLogMapper;

    @Override
    public void onLog(LogMessage logMessage) {
        JobLog jobLog = convertToEntity(logMessage);
        jobLogMapper.save(jobLog);
    }

    @Override
    public void onBatchLog(List<LogMessage> logs) {
        List<JobLog> entities = logs.stream()
                .map(this::convertToEntity)
                .toList();
        jobLogMapper.saveAll(entities);
        log.debug("批量保存日志到数据库，数量: {}", entities.size());
    }

    @Override
    public int getOrder() {
        return 10; // 数据库存储优先级较高
    }

    @Override
    public boolean isAsync() {
        return true; // 异步存储，不阻塞主流程
    }

    private JobLog convertToEntity(LogMessage logMessage) {
        return JobLog.builder()
            .jobId(logMessage.getJobId())
            .instanceId(logMessage.getInstanceId())
            .logLevel(LogLevelEnum.fromTransportLevel(logMessage.getLevel()))
            .content(logMessage.getContent())
            .build();
    }
}

