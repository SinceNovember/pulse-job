package com.simple.pulsejob.timer;

import java.util.concurrent.TimeUnit;

public interface Timer {

    /**
     * 创建一个定时任务
     * @param task 实际执行的任务
     * @param delay 延迟时间
     * @param unit 时间单位
     * @return
     */
    Timeout newTimeout(TimerTask task, long delay, TimeUnit unit);
}
