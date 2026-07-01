package com.xidian.osqa.config;

import com.xidian.osqa.service.ClazzService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ClassExpireTask {

    private static final Logger log = LoggerFactory.getLogger(ClassExpireTask.class);

    private final ClazzService clazzService;

    public ClassExpireTask(ClazzService clazzService) {
        this.clazzService = clazzService;
    }

    // 每分钟检查一次过期班级
    @Scheduled(fixedRate = 60000)
    public void checkExpiredClasses() {
        int count = clazzService.dissolveExpiredClasses();
        if (count > 0) {
            log.info("自动解散了 {} 个过期班级", count);
        }
    }
}
