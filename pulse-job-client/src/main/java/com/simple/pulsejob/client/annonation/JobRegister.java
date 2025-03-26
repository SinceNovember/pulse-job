package com.simple.pulsejob.client.annonation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 定时任务注解
 * 用于标记和配置分布式定时任务
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JobRegister {

    /**
     * 任务名称
     * 可选，默认使用方法名
     */
    String value() default "";

    /**
     * 执行任务前调用的方法名称
     */
    String init();

    /**
     * 任务执行后调用的方法名称
     */
    String destroy();

    /**
     * CRON表达式
     * 定义任务执行时间
     */
    String cron() default "";

    /**
     * 固定延迟执行
     * 上一次执行完成后，延迟指定时间再执行
     */
    long fixedDelay() default -1;

    /**
     * 固定频率执行
     * 按照固定间隔时间执行
     */
    long fixedRate() default -1;

    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 最大重试次数
     */
    int retryTimes() default 3;

    /**
     * 任务超时时间
     * 0或负数表示不超时
     */
    long timeout() default 0;

    /**
     * 描述信息
     */
    String description() default "";
}
