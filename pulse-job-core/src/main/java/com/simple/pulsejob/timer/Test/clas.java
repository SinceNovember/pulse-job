package com.simple.pulsejob.timer.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import com.simple.pulsejob.timer.HashedWheelTimer;
import com.simple.pulsejob.timer.TimerTask;

public class clas {
    public static void main(String[] args) {
        System.out.println(LocalDateTime.now());
        HashedWheelTimer hashedWheelTimer = new HashedWheelTimer();
        TimerTask timerTask = timeout ->  System.out.println(LocalDateTime.now() + "23455");
        hashedWheelTimer.newTimeout(timerTask, 5, TimeUnit.SECONDS);
        hashedWheelTimer.newTimeout(timerTask, 7, TimeUnit.SECONDS);

        for (; ; ) {

        }
    }
}
