package com.simple.pulsejob.common.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public final class StackTraceUtil {

    public static String stackTrace(Throwable t) {
        if (t == null) {
            return "null";
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(out);
        t.printStackTrace(ps);
        ps.flush();
        try {
            return new String(out.toByteArray());
        } finally {
            try {
                out.close();
            } catch (IOException ignored) {}
        }
    }

    /**
     * 获取异常堆栈的前 N 行
     *
     * @param t     异常
     * @param lines 行数
     * @return 截取后的堆栈信息
     */
    public static String stackTrace(Throwable t, int lines) {
        String fullTrace = stackTrace(t);
        if (fullTrace == null || lines <= 0) {
            return fullTrace;
        }

        String[] traceLines = fullTrace.split("\n");
        if (traceLines.length <= lines) {
            return fullTrace;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines; i++) {
            sb.append(traceLines[i]);
            if (i < lines - 1) {
                sb.append("\n");
            }
        }
        sb.append("\n... ").append(traceLines.length - lines).append(" more lines");
        return sb.toString();
    }

    /**
     * 获取异常堆栈的前 5 行（常用快捷方法）
     *
     * @param t 异常
     * @return 前 5 行堆栈信息
     */
    public static String stackTraceBrief(Throwable t) {
        return stackTrace(t, 5);
    }

    private StackTraceUtil() {}
}
