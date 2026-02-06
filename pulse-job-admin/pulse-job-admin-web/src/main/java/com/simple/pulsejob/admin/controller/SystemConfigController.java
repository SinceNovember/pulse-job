package com.simple.pulsejob.admin.controller;

import com.simple.pulsejob.admin.business.service.ISystemConfigService;
import com.simple.pulsejob.admin.common.model.base.ResponseResult;
import com.simple.pulsejob.admin.common.model.entity.SystemConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统配置控制器
 * AI Generated
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/systemConfig")
public class SystemConfigController {

    private final ISystemConfigService systemConfigService;

    /**
     * 获取所有配置
     */
    @GetMapping
    public ResponseResult<List<SystemConfig>> getAllConfigs() {
        return ResponseResult.ok(systemConfigService.getAllConfigs());
    }

    /**
     * 根据分组获取配置
     */
    @GetMapping("/group/{group}")
    public ResponseResult<List<SystemConfig>> getConfigsByGroup(@PathVariable("group") String group) {
        return ResponseResult.ok(systemConfigService.getConfigsByGroup(group));
    }

    /**
     * 根据键获取配置
     */
    @GetMapping("/key/{key}")
    public ResponseResult<SystemConfig> getConfigByKey(@PathVariable("key") String key) {
        return ResponseResult.of(systemConfigService.getConfigByKey(key), "配置不存在");
    }

    /**
     * 保存或更新单个配置
     */
    @PostMapping
    public ResponseResult<SystemConfig> saveConfig(@RequestBody SystemConfig config) {
        try {
            SystemConfig saved = systemConfigService.saveConfig(config);
            return ResponseResult.ok(saved);
        } catch (Exception e) {
            log.error("保存配置失败", e);
            return ResponseResult.error("保存配置失败: " + e.getMessage());
        }
    }

    /**
     * 批量更新配置
     */
    @PutMapping("/batch")
    public ResponseResult<Void> batchUpdateConfigs(@RequestBody Map<String, String> configs) {
        try {
            systemConfigService.batchUpdateConfigs(configs);
            return ResponseResult.ok();
        } catch (Exception e) {
            log.error("批量更新配置失败", e);
            return ResponseResult.error("批量更新配置失败: " + e.getMessage());
        }
    }

    /**
     * 删除配置
     */
    @DeleteMapping("/key/{key}")
    public ResponseResult<Void> deleteConfig(@PathVariable("key") String key) {
        try {
            systemConfigService.deleteConfig(key);
            return ResponseResult.ok();
        } catch (Exception e) {
            log.error("删除配置失败", e);
            return ResponseResult.error("删除配置失败: " + e.getMessage());
        }
    }

    /**
     * 获取注册相关配置的快捷接口
     */
    @GetMapping("/register")
    public ResponseResult<Map<String, Object>> getRegisterConfigs() {
        Map<String, Object> configs = Map.of(
                "autoRegisterExecutor", systemConfigService.isAutoRegisterExecutorEnabled(),
                "autoRegisterJob", systemConfigService.isAutoRegisterJobEnabled()
        );
        return ResponseResult.ok(configs);
    }

    /**
     * 更新注册相关配置的快捷接口
     */
    @PutMapping("/register")
    public ResponseResult<Void> updateRegisterConfigs(@RequestBody Map<String, Boolean> configs) {
        try {
            if (configs.containsKey("autoRegisterExecutor")) {
                SystemConfig config = new SystemConfig();
                config.setConfigKey(SystemConfig.KEY_AUTO_REGISTER_EXECUTOR);
                config.setConfigValue(String.valueOf(configs.get("autoRegisterExecutor")));
                config.setConfigDesc("是否启用执行器自动注册，启用后执行器连接时会自动注册到数据库");
                config.setConfigGroup(SystemConfig.GROUP_REGISTER);
                systemConfigService.saveConfig(config);
            }
            if (configs.containsKey("autoRegisterJob")) {
                SystemConfig config = new SystemConfig();
                config.setConfigKey(SystemConfig.KEY_AUTO_REGISTER_JOB);
                config.setConfigValue(String.valueOf(configs.get("autoRegisterJob")));
                config.setConfigDesc("是否启用任务自动注册，启用后执行器上报的任务处理器会自动注册为任务");
                config.setConfigGroup(SystemConfig.GROUP_REGISTER);
                systemConfigService.saveConfig(config);
            }
            return ResponseResult.ok();
        } catch (Exception e) {
            log.error("更新注册配置失败", e);
            return ResponseResult.error("更新注册配置失败: " + e.getMessage());
        }
    }
}
