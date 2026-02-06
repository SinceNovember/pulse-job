package com.simple.pulsejob.admin.persistence.mapper;

import com.simple.pulsejob.admin.common.model.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 系统配置数据访问层
 * AI Generated
 */
@Repository
public interface SystemConfigMapper extends JpaRepository<SystemConfig, Integer> {

    /**
     * 根据配置键查找配置
     */
    Optional<SystemConfig> findByConfigKey(String configKey);

    /**
     * 根据配置分组查找配置列表
     */
    List<SystemConfig> findByConfigGroup(String configGroup);

    /**
     * 检查配置键是否存在
     */
    boolean existsByConfigKey(String configKey);
}
