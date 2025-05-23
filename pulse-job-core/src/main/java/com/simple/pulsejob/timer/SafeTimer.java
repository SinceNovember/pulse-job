//package com.simple.pulsejob.timer;
//
//import io.netty.util.HashedWheelTimer;
//import io.netty.util.Timeout;
//import io.netty.util.TimerTask;
//
//import java.util.concurrent.RejectedExecutionException;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicInteger;
//
//public class SafeTimer {
//    private final HashedWheelTimer timer;
//    private final int maxPendingTasks;
//    private final AtomicInteger pendingTasks = new AtomicInteger(0);
//
//    public void addTask(TimerTask task, long delay) {
//        if (pendingTasks.get() > maxPendingTasks) {
//            throw new RejectedExecutionException("Too many pending tasks");
//        }
//
//        pendingTasks.incrementAndGet();
//        timer.newTimeout(timeout -> {
//            try {
//                task.run();
//            } finally {
//                pendingTasks.decrementAndGet();
//            }
//        }, delay, TimeUnit.MILLISECONDS);
//    }
//}