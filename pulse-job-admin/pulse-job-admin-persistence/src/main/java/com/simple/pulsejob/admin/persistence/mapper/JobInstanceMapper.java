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

}

