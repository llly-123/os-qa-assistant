package com.xidian.osqa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OsQaAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(OsQaAssistantApplication.class, args);
    }
}
