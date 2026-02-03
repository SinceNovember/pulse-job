package com.simple.pulsejob.admin.common.model.base;

import lombok.Data;

/**
 * 分页查询请求基类
 */
@Data
public class PageQuery {
    
    /**
     * 当前页码（从1开始）
     */
    private Integer page = 1;
    
    /**
     * 每页条数
     */
    private Integer pageSize = 10;
    
    /**
     * 排序字段
     */
    private String sortField;
    
    /**
     * 排序方向：asc/desc
     */
    private String sortOrder = "desc";
    
    /**
     * 获取偏移量
     */
    public int getOffset() {
        return (page - 1) * pageSize;
    }
    
    /**
     * 获取 Spring Data 的页码（从0开始）
     */
    public int getPageIndex() {
        return Math.max(0, page - 1);
    }
}
