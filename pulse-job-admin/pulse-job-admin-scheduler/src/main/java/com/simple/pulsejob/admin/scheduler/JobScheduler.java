package com.simple.pulsejob.admin.scheduler;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import com.simple.pulsejob.admin.scheduler.timer.HashedWheelTimer;
import com.simple.pulsejob.admin.scheduler.timer.Timer;
import com.simple.pulsejob.admin.scheduler.timer.TimerTask;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobScheduler {

    private final Timer hashedWheelTimer;

    public void schedule(Runnable task) {
        TimerTask timerTask = timeout -> {
            System.out.println("111");
            System.out.println(timeout);
            task.run();
        };

        hashedWheelTimer.newTimeout(timerTask, 10, TimeUnit.SECONDS);
    }

}
