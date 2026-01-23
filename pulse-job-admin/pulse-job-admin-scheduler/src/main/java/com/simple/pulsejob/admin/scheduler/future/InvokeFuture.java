package com.simple.pulsejob.admin.scheduler.future;

import com.simple.pulsejob.transport.JResponse;

import java.util.concurrent.CompletionStage;

/**
 * 任务调用 Future 接口
 * 支持流式日志接收
 */
public interface InvokeFuture extends CompletionStage<JResponse> {
    
    /**
     * 获取任务执行结果
     */
    JResponse getResult() throws Throwable;
    
}
