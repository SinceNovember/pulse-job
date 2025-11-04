//package com.simple.pulsejob.timer.Test;
//
//import com.simple.pulsejob.common.util.timer.HashedWheelTimer;
//import com.simple.pulsejob.common.util.timer.Timer;
//import com.simple.pulsejob.common.util.timer.TimerTask;
//
//import java.time.LocalDateTime;
//import java.util.concurrent.TimeUnit;
//
//public class clas {
//    public static void main(String[] args) {
//        System.out.println(LocalDateTime.now());
//        Timer hashedWheelTimer = new HashedWheelTimer();
//        TimerTask timerTask = timeout ->  System.out.println(LocalDateTime.now() + "23455");
//        hashedWheelTimer.newTimeout(timerTask, 5, TimeUnit.SECONDS);
//        hashedWheelTimer.newTimeout(timerTask, 7, TimeUnit.SECONDS);
//
//        for (; ; ) {
//        }
//    }
//}
