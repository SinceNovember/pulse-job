package com.simple.pulsejob.common.util;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Job 名称解析器.
 *
 * <p>提供统一的 Job 命名规则，支持多种命名策略：</p>
 * <ul>
 *   <li>指定名称 - 使用注解中明确指定的名称</li>
 *   <li>方法签名 - 类名#方法名(参数类型...)</li>
 *   <li>简单名称 - 类名#方法名</li>
 *   <li>全限定名 - 完整包名.类名#方法名(参数类型...)</li>
 * </ul>
 *
 * <pre>{@code
 * // 使用示例
 * String name = JobNameResolver.resolve(method, annotationValue);
 * String name = JobNameResolver.fromMethod(method);
 * String name = JobNameResolver.fromMethod(method, Strategy.SIMPLE);
 * }</pre>
 */
public final class JobNameResolver {

    private JobNameResolver() {}

    /**
     * 命名策略
     */
    public enum Strategy {
        /** 方法签名：类名#方法名(参数类型...) */
        METHOD_SIGNATURE,
        /** 简单名称：类名#方法名 */
        SIMPLE,
        /** 全限定名：完整包名.类名#方法名(参数类型...) */
        FULLY_QUALIFIED
    }

    /** 默认策略 */
    private static final Strategy DEFAULT_STRATEGY = Strategy.METHOD_SIGNATURE;

    /**
     * 解析 Job 名称.
     *
     * <p>如果指定了名称则使用指定名称，否则根据方法自动生成</p>
     *
     * @param method         方法
     * @param specifiedName  注解中指定的名称（可为空）
     * @return Job 名称
     */
    public static String resolve(Method method, String specifiedName) {
        return resolve(method, specifiedName, DEFAULT_STRATEGY);
    }

    /**
     * 解析 Job 名称（使用指定策略）.
     *
     * @param method         方法
     * @param specifiedName  注解中指定的名称（可为空）
     * @param strategy       命名策略
     * @return Job 名称
     */
    public static String resolve(Method method, String specifiedName, Strategy strategy) {
        if (StringUtil.isNotBlank(specifiedName)) {
            return specifiedName.trim();
        }
        return fromMethod(method, strategy);
    }

    /**
     * 从方法生成 Job 名称（使用默认策略）.
     *
     * @param method 方法
     * @return Job 名称
     */
    public static String fromMethod(Method method) {
        return fromMethod(method, DEFAULT_STRATEGY);
    }

    /**
     * 从方法生成 Job 名称.
     *
     * @param method   方法
     * @param strategy 命名策略
     * @return Job 名称
     */
    public static String fromMethod(Method method, Strategy strategy) {
        if (method == null) {
            throw new IllegalArgumentException("Method must not be null");
        }

        return switch (strategy) {
            case SIMPLE -> buildSimpleName(method);
            case FULLY_QUALIFIED -> buildFullyQualifiedName(method);
            case METHOD_SIGNATURE -> buildMethodSignature(method);
        };
    }

    /**
     * 从类和方法名生成 Job 名称.
     *
     * @param clazz      类
     * @param methodName 方法名
     * @return Job 名称
     */
    public static String fromClass(Class<?> clazz, String methodName) {
        return clazz.getSimpleName() + Strings.HASH_SYMBOL + methodName;
    }

    /**
     * 构建方法签名名称：类名#方法名(参数类型...)
     */
    private static String buildMethodSignature(Method method) {
        String params = Arrays.stream(method.getParameterTypes())
                .map(Class::getSimpleName)
                .collect(Collectors.joining(Strings.COMMA, Strings.LEFT_PAREN, Strings.RIGHT_PAREN));

        return method.getDeclaringClass().getSimpleName()
                + Strings.HASH_SYMBOL
                + method.getName()
                + params;
    }

    /**
     * 构建简单名称：类名#方法名
     */
    private static String buildSimpleName(Method method) {
        return method.getDeclaringClass().getSimpleName()
                + Strings.HASH_SYMBOL
                + method.getName();
    }

    /**
     * 构建全限定名称：完整包名.类名#方法名(参数类型...)
     */
    private static String buildFullyQualifiedName(Method method) {
        String params = Arrays.stream(method.getParameterTypes())
                .map(Class::getName)
                .collect(Collectors.joining(Strings.COMMA, Strings.LEFT_PAREN, Strings.RIGHT_PAREN));

        return method.getDeclaringClass().getName()
                + Strings.HASH_SYMBOL
                + method.getName()
                + params;
    }

    /**
     * 解析 Job 名称，提取类名和方法名.
     *
     * @param jobName Job 名称
     * @return [类名, 方法名]，如果解析失败返回 null
     */
    public static String[] parse(String jobName) {
        if (StringUtil.isBlank(jobName)) {
            return null;
        }

        int hashIndex = jobName.indexOf(Strings.HASH_SYMBOL);
        if (hashIndex <= 0) {
            return null;
        }

        String className = jobName.substring(0, hashIndex);
        String methodPart = jobName.substring(hashIndex + 1);

        // 移除参数部分
        int parenIndex = methodPart.indexOf(Strings.LEFT_PAREN);
        String methodName = parenIndex > 0 ? methodPart.substring(0, parenIndex) : methodPart;

        return new String[]{className, methodName};
    }

    /**
     * 验证 Job 名称是否有效.
     *
     * @param jobName Job 名称
     * @return true 如果有效
     */
    public static boolean isValid(String jobName) {
        if (StringUtil.isBlank(jobName)) {
            return false;
        }
        // 必须包含 # 分隔符，且类名和方法名都不为空
        String[] parts = parse(jobName);
        return parts != null 
                && StringUtil.isNotBlank(parts[0]) 
                && StringUtil.isNotBlank(parts[1]);
    }
}

