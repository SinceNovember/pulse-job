package com.simple.pulsejob.timer;

public interface TimerTask {

    void run(Timeout timeout) throws Exception;

}
