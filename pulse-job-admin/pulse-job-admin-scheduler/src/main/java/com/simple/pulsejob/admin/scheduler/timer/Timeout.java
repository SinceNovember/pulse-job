package com.simple.pulsejob.admin.scheduler.timer;

public interface Timeout {

    Timer timer();

    TimerTask task();

    boolean isExpired();

    boolean isCancelled();

    boolean cancel();
}
