package com.simple.pulsejob.common.util.timer;

public interface TimerTask {

    void run(Timeout timeout) throws Exception;

}
