package com.xidian.osqa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class OsQaAssistantApplication {

    public static void main(String[] args) {
        // 固定 JVM 默认时区为北京时间，避免在 UTC 服务器上 LocalDateTime.now()/LocalDate.now()
        // 取到 UTC 时间，导致统计的时段/日期整体偏移 8 小时
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        SpringApplication.run(OsQaAssistantApplication.class, args);
    }
}
