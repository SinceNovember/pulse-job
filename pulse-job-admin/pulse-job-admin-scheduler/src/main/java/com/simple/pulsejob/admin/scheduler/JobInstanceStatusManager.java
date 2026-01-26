package com.simple.pulsejob.admin.scheduler;

import com.simple.pulsejob.admin.common.model.enums.JobInstanceStatus;
import com.simple.pulsejob.admin.persistence.mapper.JobInstanceMapper;
import com.simple.pulsejob.common.util.StackTraceUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 任务实例状态管理器.
 *
 * <p>统一管理 JobInstance 的状态更新，包括：</p>
 * <ul>
 *   <li>TRANSPORTED - 已发送到执行器</li>
 *   <li>TRANSPORT_FAILED - 发送失败</li>
 *   <li>SUCCESS - 执行成功</li>
 *   <li>FAILED - 执行失败</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobInstanceStatusManager {

    private final JobInstanceMapper jobInstanceMapper;

    // ==================== 传输阶段 ====================

    /**
     * 标记为已发送
     */
    public void markTransported(Long instanceId) {
        updateStatus(instanceId, JobInstanceStatus.TRANSPORTED);
    }

    /**
     * 标记为发送失败
     */
    public void markTransportFailed(Long instanceId) {
        updateStatus(instanceId, JobInstanceStatus.TRANSPORT_FAILED);
    }

    // ==================== 执行阶段 ====================

    /**
     * 标记为执行超时
     */
    public void markTimeout(Long instanceId) {
        markTimeout(instanceId, "Task execution timeout");
    }

    /**
     * 标记为执行超时
     *
     * @param instanceId   实例ID
     * @param errorMessage 超时信息
     */
    public void markTimeout(Long instanceId, String errorMessage) {
        if (instanceId == null) {
            log.warn("跳过状态更新: instanceId is null");
            return;
        }
        try {
            int rows = jobInstanceMapper.updateStatusWithError(
                    instanceId,
                    JobInstanceStatus.TIMEOUT.getValue(),
                    errorMessage
            );
            logUpdateResult(instanceId, JobInstanceStatus.TIMEOUT, rows);
        } catch (Exception e) {
            log.error("更新实例状态异常: instanceId={}, status={}", instanceId, JobInstanceStatus.TIMEOUT, e);
        }
    }

    /**
     * 标记为执行成功
     *
     * @param instanceId 实例ID
     * @param result     执行结果（JSON字符串）
     */
    public void markSuccess(Long instanceId, String result) {
        if (instanceId == null) {
            log.warn("跳过状态更新: instanceId is null");
            return;
        }
        try {
            int rows = jobInstanceMapper.updateStatusWithResult(
                    instanceId,
                    JobInstanceStatus.SUCCESS.getValue(),
                    result
            );
            logUpdateResult(instanceId, JobInstanceStatus.SUCCESS, rows);
        } catch (Exception e) {
            log.error("更新实例状态异常: instanceId={}, status={}", instanceId, JobInstanceStatus.SUCCESS, e);
        }
    }

    /**
     * 标记为执行失败
     *
     * @param instanceId 实例ID
     * @param throwable  异常信息
     */
    public void markFailed(Long instanceId, Throwable throwable) {
        markFailed(instanceId, StackTraceUtil.stackTraceBrief(throwable));
    }

    /**
     * 标记为执行失败
     *
     * @param instanceId   实例ID
     * @param errorMessage 错误信息
     */
    public void markFailed(Long instanceId, String errorMessage) {
        if (instanceId == null) {
            log.warn("跳过状态更新: instanceId is null");
            return;
        }
        try {
            int rows = jobInstanceMapper.updateStatusWithError(
                    instanceId,
                    JobInstanceStatus.FAILED.getValue(),
                    errorMessage
            );
            logUpdateResult(instanceId, JobInstanceStatus.FAILED, rows);
        } catch (Exception e) {
            log.error("更新实例状态异常: instanceId={}, status={}", instanceId, JobInstanceStatus.FAILED, e);
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 更新状态（仅状态，不带附加信息）
     */
    private void updateStatus(Long instanceId, JobInstanceStatus status) {
        if (instanceId == null) {
            log.warn("跳过状态更新: instanceId is null");
            return;
        }
        try {
            int rows = jobInstanceMapper.updateStatus(instanceId, status.getValue());
            logUpdateResult(instanceId, status, rows);
        } catch (Exception e) {
            log.error("更新实例状态异常: instanceId={}, status={}", instanceId, status, e);
        }
    }

    private void logUpdateResult(Long instanceId, JobInstanceStatus status, int rows) {
        if (rows > 0) {
            log.debug("实例状态更新: instanceId={}, status={}", instanceId, status);
        } else {
            log.warn("实例状态更新失败: instanceId={}, status={}", instanceId, status);
        }
    }
}
