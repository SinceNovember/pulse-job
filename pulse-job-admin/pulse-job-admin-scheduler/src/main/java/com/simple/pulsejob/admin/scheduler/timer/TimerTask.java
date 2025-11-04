package com.simple.pulsejob.admin.scheduler.timer;

public interface TimerTask {

    void run(Timeout timeout) throws Exception;

}
