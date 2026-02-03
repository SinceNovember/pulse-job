package com.simple.pulsejob.admin.common.model.base;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.Collections;
import java.util.List;

/**
 * 分页查询结果
 */
@Data
public class PageResult<T> {
    
    /**
     * 当前页码（从1开始）
     */
    private Integer page;
    
    /**
     * 每页条数
     */
    private Integer pageSize;
    
    /**
     * 总记录数
     */
    private Long total;
    
    /**
     * 总页数
     */
    private Integer totalPages;
    
    /**
     * 数据列表
     */
    private List<T> list;
    
    /**
     * 是否有下一页
     */
    private Boolean hasNext;
    
    /**
     * 是否有上一页
     */
    private Boolean hasPrevious;
    
    public PageResult() {
    }
    
    public PageResult(Integer page, Integer pageSize, Long total, List<T> list) {
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
        this.list = list;
        this.totalPages = (int) Math.ceil((double) total / pageSize);
        this.hasNext = page < totalPages;
        this.hasPrevious = page > 1;
    }
    
    /**
     * 从 Spring Data Page 转换
     */
    public static <T> PageResult<T> of(Page<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setPage(page.getNumber() + 1); // Spring Data 页码从0开始
        result.setPageSize(page.getSize());
        result.setTotal(page.getTotalElements());
        result.setTotalPages(page.getTotalPages());
        result.setList(page.getContent());
        result.setHasNext(page.hasNext());
        result.setHasPrevious(page.hasPrevious());
        return result;
    }
    
    /**
     * 创建空分页结果
     */
    public static <T> PageResult<T> empty(Integer page, Integer pageSize) {
        PageResult<T> result = new PageResult<>();
        result.setPage(page);
        result.setPageSize(pageSize);
        result.setTotal(0L);
        result.setTotalPages(0);
        result.setList(Collections.emptyList());
        result.setHasNext(false);
        result.setHasPrevious(false);
        return result;
    }
}
