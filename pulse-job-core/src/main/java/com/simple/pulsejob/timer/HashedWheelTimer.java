package com.simple.pulsejob.timer;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public class HashedWheelTimer implements Timer{

    private static final AtomicIntegerFieldUpdater<HashedWheelTimer> workerStateUpdater =
            AtomicIntegerFieldUpdater.newUpdater(HashedWheelTimer.class, "workerState");


    @Override
    public Timeout newTimeout(TimerTask task, long delay, TimeUnit unit) {
        return null;
    }

    private final class Worker implements Runnable {

        private final Set<Timeout> unprocessedTimeouts = new HashSet<>();

        private long tick;
        @Override
        public void run() {

        }
    }
}
