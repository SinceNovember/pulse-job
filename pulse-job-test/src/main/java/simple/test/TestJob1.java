package simple.test;

import com.simple.pulsejob.client.annonation.JobRegister;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TestJob1 {

    @JobRegister(cron = "111")
    public String testJob1() {
        log.info("log------1");

        log.error("log------2");

        log.warn("log-------3");

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        log.info("log------1");

        log.info("log------1");

        log.info("log------1");
        log.info("log------1");
        return "test Job result";

    }
}
