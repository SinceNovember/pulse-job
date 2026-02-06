package com.simple.pulsejob.admin.common.model.entity;

import lombok.Data;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统配置实体
 * AI Generated
 */
@Data
@Entity
@Table(name = "system_config")
public class SystemConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * 配置键
     */
    @Column(name = "config_key", length = 100, nullable = false, unique = true)
    private String configKey;

    /**
     * 配置值
     */
    @Column(name = "config_value", length = 500)
    private String configValue;

    /**
     * 配置描述
     */
    @Column(name = "config_desc", length = 200)
    private String configDesc;

    /**
     * 配置分组
     */
    @Column(name = "config_group", length = 50)
    private String configGroup;

    /**
     * 创建时间
     */
    @Column(name = "create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    /**
     * 常用配置键定义
     */
    public static final String KEY_AUTO_REGISTER_EXECUTOR = "auto_register_executor";
    public static final String KEY_AUTO_REGISTER_JOB = "auto_register_job";

    public static final String GROUP_REGISTER = "register";

    public static SystemConfig of(String key, String value, String desc, String group) {
        SystemConfig config = new SystemConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setConfigDesc(desc);
        config.setConfigGroup(group);
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        return config;
    }
}
