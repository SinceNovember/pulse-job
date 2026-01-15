package simple.test;

import com.simple.pulsejob.client.annonation.JobRegister;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TestJob2 {

    @JobRegister(cron = "111")
    public void testJob1() {
        log.info("log2------1");

        log.error("log2------2");

        log.warn("log2-------3");

        log.info("log2------1");

        log.info("log2------1");

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        log.info("log2------1");
        log.info("log2------1");

    }
}
