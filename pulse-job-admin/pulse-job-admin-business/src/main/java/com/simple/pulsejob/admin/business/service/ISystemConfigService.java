package com.simple.pulsejob.admin.business.service;

import com.simple.pulsejob.admin.common.model.entity.SystemConfig;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 系统配置服务接口
 * AI Generated
 */
public interface ISystemConfigService {

    /**
     * 获取所有配置
     */
    List<SystemConfig> getAllConfigs();

    /**
     * 根据分组获取配置
     */
    List<SystemConfig> getConfigsByGroup(String group);

    /**
     * 根据键获取配置
     */
    Optional<SystemConfig> getConfigByKey(String key);

    /**
     * 获取配置值，如果不存在返回默认值
     */
    String getConfigValue(String key, String defaultValue);

    /**
     * 获取布尔类型配置值
     */
    boolean getBooleanConfig(String key, boolean defaultValue);

    /**
     * 保存或更新配置
     */
    SystemConfig saveConfig(SystemConfig config);

    /**
     * 批量更新配置
     */
    void batchUpdateConfigs(Map<String, String> configs);

    /**
     * 删除配置
     */
    void deleteConfig(String key);

    /**
     * 初始化默认配置（如果不存在）
     */
    void initDefaultConfigs();

    // ========== 便捷方法 ==========

    /**
     * 是否启用执行器自动注册
     */
    boolean isAutoRegisterExecutorEnabled();

    /**
     * 是否启用任务自动注册
     */
    boolean isAutoRegisterJobEnabled();
}
