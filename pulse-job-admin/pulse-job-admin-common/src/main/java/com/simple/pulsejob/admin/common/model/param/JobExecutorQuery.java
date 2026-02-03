package com.simple.pulsejob.admin.common.model.param;

import com.simple.pulsejob.admin.common.model.base.PageQuery;
import com.simple.pulsejob.admin.common.model.enums.RegisterTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 执行器分页查询参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class JobExecutorQuery extends PageQuery {
    
    /**
     * 执行器名称（模糊查询）
     */
    private String executorName;
    
    /**
     * 注册方式
     */
    private RegisterTypeEnum registerType;
    
    /**
     * 状态筛选：online/offline（前端传入，后端不处理）
     */
    private String status;
}
