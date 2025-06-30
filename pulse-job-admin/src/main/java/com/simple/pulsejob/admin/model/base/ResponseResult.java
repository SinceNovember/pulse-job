package com.simple.pulsejob.admin.model.base;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;
import java.util.Optional;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponseResult<T> {

    private int code;
    private String message;
    private T data;

    private ResponseResult(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // 成功响应
    public static <T> ResponseResult<T> ok(T data) {
        return new ResponseResult<>(200, "操作成功", data);
    }

    public static <T> ResponseResult<T> ok() {
        return new ResponseResult<>(200, "操作成功", null);
    }

    public static <T> ResponseResult<T> ok(String message, T data) {
        return new ResponseResult<>(200, message, data);
    }

    // 创建成功
    public static <T> ResponseResult<T> created(T data) {
        return new ResponseResult<>(201, "创建成功", data);
    }

    // 客户端错误
    public static <T> ResponseResult<T> badRequest(String message) {
        return new ResponseResult<>(400, message, null);
    }

    public static <T> ResponseResult<T> badRequest() {
        return new ResponseResult<>(400, "请求参数错误", null);
    }

    public static <T> ResponseResult<T> notFound() {
        return new ResponseResult<>(404, "资源未找到", null);
    }

    public static <T> ResponseResult<T> notFound(String message) {
        return new ResponseResult<>(404, message, null);
    }

    // 服务器错误
    public static <T> ResponseResult<T> error() {
        return new ResponseResult<>(500, "服务器内部错误", null);
    }

    public static <T> ResponseResult<T> error(String message) {
        return new ResponseResult<>(500, message, null);
    }

    // 处理Optional
    public static <T> ResponseResult<T> of(Optional<T> optional) {
        return optional.map(ResponseResult::ok)
            .orElse(notFound());
    }

    public static <T> ResponseResult<T> of(Optional<T> optional, String notFoundMessage) {
        return optional.map(ResponseResult::ok)
            .orElse(notFound(notFoundMessage));
    }

    // 处理列表
    public static <T> ResponseResult<List<T>> okList(List<T> list) {
        return ok(list);
    }

    // 自定义状态码
    public static <T> ResponseResult<T> custom(int code, String message, T data) {
        return new ResponseResult<>(code, message, data);
    }

    // 判断是否成功
    public boolean isSuccess() {
        return this.code >= 200 && this.code < 300;
    }
}