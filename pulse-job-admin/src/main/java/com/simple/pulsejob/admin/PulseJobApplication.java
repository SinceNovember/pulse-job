package com.simple.pulsejob.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@RequiredArgsConstructor
@SpringBootApplication
public class PulseJobApplication implements ApplicationRunner {

    private final DefaultServer defaultServer;
    public static void main(String[] args) {
        SpringApplication.run(PulseJobApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        defaultServer.start();
    }
}
