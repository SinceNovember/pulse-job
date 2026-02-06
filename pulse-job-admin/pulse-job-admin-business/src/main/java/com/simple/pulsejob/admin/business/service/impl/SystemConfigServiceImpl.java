package com.simple.pulsejob.admin.business.service.impl;

import com.simple.pulsejob.admin.business.service.ISystemConfigService;
import com.simple.pulsejob.admin.common.model.entity.SystemConfig;
import com.simple.pulsejob.admin.persistence.mapper.SystemConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统配置服务实现
 * AI Generated
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements ISystemConfigService {

    private final SystemConfigMapper systemConfigMapper;

    /**
     * 配置缓存，避免频繁查询数据库
     */
    private final Map<String, String> configCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        initDefaultConfigs();
        refreshCache();
    }

    @Override
    public List<SystemConfig> getAllConfigs() {
        return systemConfigMapper.findAll();
    }

    @Override
    public List<SystemConfig> getConfigsByGroup(String group) {
        return systemConfigMapper.findByConfigGroup(group);
    }

    @Override
    public Optional<SystemConfig> getConfigByKey(String key) {
        return systemConfigMapper.findByConfigKey(key);
    }

    @Override
    public String getConfigValue(String key, String defaultValue) {
        // 先从缓存获取
        String cached = configCache.get(key);
        if (cached != null) {
            return cached;
        }
        // 缓存未命中，查询数据库
        return systemConfigMapper.findByConfigKey(key)
                .map(SystemConfig::getConfigValue)
                .orElse(defaultValue);
    }

    @Override
    public boolean getBooleanConfig(String key, boolean defaultValue) {
        String value = getConfigValue(key, null);
        if (value == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SystemConfig saveConfig(SystemConfig config) {
        SystemConfig existing = systemConfigMapper.findByConfigKey(config.getConfigKey()).orElse(null);
        if (existing != null) {
            existing.setConfigValue(config.getConfigValue());
            existing.setConfigDesc(config.getConfigDesc());
            existing.setConfigGroup(config.getConfigGroup());
            existing.setUpdateTime(LocalDateTime.now());
            SystemConfig saved = systemConfigMapper.save(existing);
            // 更新缓存
            configCache.put(config.getConfigKey(), config.getConfigValue());
            log.info("配置已更新: {} = {}", config.getConfigKey(), config.getConfigValue());
            return saved;
        } else {
            config.setCreateTime(LocalDateTime.now());
            config.setUpdateTime(LocalDateTime.now());
            SystemConfig saved = systemConfigMapper.save(config);
            // 更新缓存
            configCache.put(config.getConfigKey(), config.getConfigValue());
            log.info("配置已创建: {} = {}", config.getConfigKey(), config.getConfigValue());
            return saved;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateConfigs(Map<String, String> configs) {
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            SystemConfig config = new SystemConfig();
            config.setConfigKey(entry.getKey());
            config.setConfigValue(entry.getValue());
            saveConfig(config);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(String key) {
        systemConfigMapper.findByConfigKey(key).ifPresent(config -> {
            systemConfigMapper.delete(config);
            configCache.remove(key);
            log.info("配置已删除: {}", key);
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void initDefaultConfigs() {
        // 初始化执行器自动注册配置
        if (!systemConfigMapper.existsByConfigKey(SystemConfig.KEY_AUTO_REGISTER_EXECUTOR)) {
            SystemConfig config = SystemConfig.of(
                    SystemConfig.KEY_AUTO_REGISTER_EXECUTOR,
                    "true",
                    "是否启用执行器自动注册，启用后执行器连接时会自动注册到数据库",
                    SystemConfig.GROUP_REGISTER
            );
            systemConfigMapper.save(config);
            log.info("初始化默认配置: {} = true", SystemConfig.KEY_AUTO_REGISTER_EXECUTOR);
        }

        // 初始化任务自动注册配置
        if (!systemConfigMapper.existsByConfigKey(SystemConfig.KEY_AUTO_REGISTER_JOB)) {
            SystemConfig config = SystemConfig.of(
                    SystemConfig.KEY_AUTO_REGISTER_JOB,
                    "false",
                    "是否启用任务自动注册，启用后执行器上报的任务处理器会自动注册为任务",
                    SystemConfig.GROUP_REGISTER
            );
            systemConfigMapper.save(config);
            log.info("初始化默认配置: {} = false", SystemConfig.KEY_AUTO_REGISTER_JOB);
        }
    }

    /**
     * 刷新配置缓存
     */
    public void refreshCache() {
        configCache.clear();
        List<SystemConfig> allConfigs = systemConfigMapper.findAll();
        for (SystemConfig config : allConfigs) {
            if (config.getConfigValue() != null) {
                configCache.put(config.getConfigKey(), config.getConfigValue());
            }
        }
        log.info("配置缓存已刷新，共 {} 条配置", configCache.size());
    }

    // ========== 便捷方法 ==========

    @Override
    public boolean isAutoRegisterExecutorEnabled() {
        return getBooleanConfig(SystemConfig.KEY_AUTO_REGISTER_EXECUTOR, true);
    }

    @Override
    public boolean isAutoRegisterJobEnabled() {
        return getBooleanConfig(SystemConfig.KEY_AUTO_REGISTER_JOB, false);
    }
}
