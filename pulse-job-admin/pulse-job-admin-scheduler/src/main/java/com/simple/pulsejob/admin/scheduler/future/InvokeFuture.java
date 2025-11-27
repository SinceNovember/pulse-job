package com.simple.pulsejob.admin.scheduler.future;

import com.simple.pulsejob.transport.JResponse;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 任务调用 Future 接口
 * 支持流式日志接收
 */
public interface InvokeFuture {
    
    /**
     * 获取任务执行结果
     */
    Object getResult() throws Throwable;
    
    /**
     * 接收日志消息（流式）
     */
    void receiveLog(LogMessage logMessage);
    
    /**
     * 添加日志监听器
     */
    InvokeFuture addLogListener(Consumer<LogMessage> logListener);
    
    /**
     * 标记任务完成
     */
    void markCompleted(Object result);
    
    /**
     * 标记任务失败
     */
    void markFailed(Throwable cause);
    
    /**
     * 获取日志历史
     */
    List<LogMessage> getLogHistory();
}
