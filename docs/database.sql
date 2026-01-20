-- pulse_job.job_executor definition

CREATE TABLE `job_executor` (
                                `id` int NOT NULL AUTO_INCREMENT,
                                `executor_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '执行器AppName',
                                `executor_desc` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '执行器名称',
                                `register_type` tinyint NOT NULL DEFAULT '0' COMMENT '执行器地址类型：0=自动注册、1=手动录入',
                                `executor_address` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci COMMENT '执行器地址列表，多地址逗号分隔',
                                `update_time` datetime DEFAULT NULL,
                                PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- pulse_job.job_info definition

CREATE TABLE `job_info` (
                            `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                            `job_handler` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '任务处理器名称',
                            `schedule_rate` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '调度频率',
                            `schedule_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '调度类型',
                            `executor_id` int DEFAULT NULL COMMENT '执行器ID',
                            `status` int DEFAULT '1' COMMENT '任务状态：0-禁用，1-启用',
                            `next_execute_time` datetime DEFAULT NULL COMMENT '下次执行时间',
                            `last_execute_time` datetime DEFAULT NULL COMMENT '上次执行时间',
                            `description` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '任务描述',
                            `create_time` datetime DEFAULT NULL COMMENT '创建时间',
                            `update_time` datetime DEFAULT NULL COMMENT '更新时间',
                            `job_params` text COLLATE utf8mb4_unicode_ci COMMENT '任务参数（JSON格式）',
                            `retry_times` int DEFAULT '0' COMMENT '重试次数',
                            `max_retry_times` int DEFAULT '3' COMMENT '最大重试次数',
                            `timeout_seconds` int DEFAULT '60' COMMENT '任务超时时间（秒）',
                            `dispatch_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                            `load_balance_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                            `serializer_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL,
                            PRIMARY KEY (`id`),
                            KEY `idx_job_handler` (`job_handler`),
                            KEY `idx_executor_id` (`executor_id`),
                            KEY `idx_status` (`status`),
                            KEY `idx_next_execute_time` (`next_execute_time`),
                            KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务信息表';

-- pulse_job.job_instance definition

CREATE TABLE `job_instance` (
                                `id` bigint NOT NULL AUTO_INCREMENT,
                                `job_id` bigint NOT NULL,
                                `executor_id` bigint NOT NULL,
                                `trigger_time` datetime NOT NULL,
                                `start_time` datetime DEFAULT NULL,
                                `end_time` datetime DEFAULT NULL,
                                `status` tinyint NOT NULL DEFAULT '0',
                                `retry_count` int DEFAULT '0',
                                `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                PRIMARY KEY (`id`),
                                KEY `idx_job_id` (`job_id`),
                                KEY `idx_executor_id` (`executor_id`)
) ENGINE=InnoDB AUTO_INCREMENT=57 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- pulse_job.job_log definition

CREATE TABLE `job_log` (
                           `id` bigint NOT NULL AUTO_INCREMENT,
                           `instance_id` bigint NOT NULL,
                           `log_level` varchar(16) NOT NULL,
                           `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
                           `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           `job_id` int DEFAULT NULL,
                           PRIMARY KEY (`id`),
                           KEY `idx_instance_id` (`instance_id`),
                           FULLTEXT KEY `ft_message` (`content`)
) ENGINE=InnoDB AUTO_INCREMENT=511 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;