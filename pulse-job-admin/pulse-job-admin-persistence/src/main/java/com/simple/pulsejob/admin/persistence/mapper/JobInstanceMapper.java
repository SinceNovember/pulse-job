package com.simple.pulsejob.admin.persistence.mapper;

import com.simple.pulsejob.admin.common.model.entity.JobInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface JobInstanceMapper extends JpaRepository<JobInstance, Long>, JpaSpecificationExecutor<JobInstance> {

    @Transactional
    @Modifying
    @Query("update JobInstance ji set ji.status = :status, ji.updateTime = CURRENT_TIMESTAMP where ji.id = :instanceId")
    int updateStatus(@Param("instanceId") Long instanceId, @Param("status") byte status);

    /**
     * 更新状态和执行结果（成功时调用）
     */
    @Transactional
    @Modifying
    @Query("update JobInstance ji set ji.status = :status, ji.result = :result, ji.endTime = CURRENT_TIMESTAMP, ji.updateTime = CURRENT_TIMESTAMP where ji.id = :instanceId")
    int updateStatusWithResult(@Param("instanceId") Long instanceId, @Param("status") byte status, @Param("result") String result);

    /**
     * 更新状态和错误信息（失败时调用）
     */
    @Transactional
    @Modifying
    @Query("update JobInstance ji set ji.status = :status, ji.errorMessage = :errorMessage, ji.endTime = CURRENT_TIMESTAMP, ji.updateTime = CURRENT_TIMESTAMP where ji.id = :instanceId")
    int updateStatusWithError(@Param("instanceId") Long instanceId, @Param("status") byte status, @Param("errorMessage") String errorMessage);

}

