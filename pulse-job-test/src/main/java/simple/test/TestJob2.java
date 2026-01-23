package simple.test;

import java.util.ArrayList;
import java.util.List;
import com.simple.pulsejob.client.annonation.JobRegister;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TestJob2 {

    @Data
    @AllArgsConstructor
    public static class TestRecord {
        private String name;

        private Integer id;
    }

    @JobRegister(cron = "111")
    public List<TestRecord> testJob1() {
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

        List<TestRecord> testRecords = new ArrayList<>();
        testRecords.add(new TestRecord("123", 14));
        testRecords.add(new TestRecord("5555aaaa", 16));
        return testRecords;

    }
}
