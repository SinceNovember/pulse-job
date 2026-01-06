package com.simple.pulsejob.admin.persistence.mapper;

import com.simple.pulsejob.admin.common.model.entity.JobInstance;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface JobInstanceMapper extends JpaRepository<JobInstance, Long>, JpaSpecificationExecutor<JobInstance> {

    List<JobInstance> findByJobIdOrderByTriggerTimeDesc(Long jobId);

    List<JobInstance> findByExecutorIdOrderByTriggerTimeDesc(Long executorId);
}

