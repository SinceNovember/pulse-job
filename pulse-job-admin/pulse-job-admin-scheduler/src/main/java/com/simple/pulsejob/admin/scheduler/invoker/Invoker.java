package com.simple.pulsejob.admin.scheduler.invoker;

/**
 * 任务调用器接口.
 *
 * <p>所有调度都会创建 JobInstance，使用 instanceId 作为请求标识</p>
 */
public interface Invoker {

    /**
     * 执行调度
     *
     * @param executorName 执行器名称
     * @param jobId        任务ID
     * @param executorId   执行器ID
     * @param handlerName  处理器名称
     * @param args         参数
     * @return 执行结果
     */
    Object invoke(String executorName, Long jobId, Long executorId,
                  String handlerName, String args) throws Throwable;
}
