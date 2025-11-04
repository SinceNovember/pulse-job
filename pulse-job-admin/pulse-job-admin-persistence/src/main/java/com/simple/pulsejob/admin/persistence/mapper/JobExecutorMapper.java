package com.simple.pulsejob.admin.persistence.mapper;

import java.util.List;
import java.util.Optional;
import com.simple.pulsejob.admin.common.model.entity.JobExecutor;
import com.simple.pulsejob.admin.common.model.enums.RegisterTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface JobExecutorMapper extends JpaRepository<JobExecutor, Integer>, JpaSpecificationExecutor<JobExecutor> {
    
    /**
     * 根据执行器名称查找执行器
     * @param executorName 执行器名称
     * @return 执行器
     */
    Optional<JobExecutor> findByExecutorName(String executorName);
    
    /**
     * 根据注册类型查找执行器
     * @param registerType 注册类型
     * @return 执行器列表
     */
    List<JobExecutor> findByRegisterType(RegisterTypeEnum registerType);

    @Transactional
    @Modifying
    @Query("UPDATE JobExecutor SET executorAddress = null")
    void updateAllExecutorAddressNull();
} 