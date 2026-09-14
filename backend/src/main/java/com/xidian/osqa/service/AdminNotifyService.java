package com.xidian.osqa.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 管理员通知邮箱服务。
 *
 * 管理员可绑定至多 10 个通知邮箱；当有新教师注册时，异步给所有通知邮箱发送邮件提醒。
 * 邮箱列表存储于 system_setting 表（key=admin_notify_emails，逗号分隔）。
 */
@Service
public class AdminNotifyService {

    private static final Logger log = LoggerFactory.getLogger(AdminNotifyService.class);

    private static final String KEY_NOTIFY_EMAILS = "admin_notify_emails";
    private static final int MAX_EMAILS = 10;
    private static final String EMAIL_REGEX = "^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$";

    private final SystemSettingService settingService;
    private final MailService mailService;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "admin-notify");
        t.setDaemon(true);
        return t;
    });

    public AdminNotifyService(SystemSettingService settingService, MailService mailService) {
        this.settingService = settingService;
        this.mailService = mailService;
    }

    /** 获取管理员通知邮箱列表（去重） */
    public List<String> getNotifyEmails() {
        String raw = settingService.get(KEY_NOTIFY_EMAILS);
        if (raw == null || raw.isBlank()) {
            return new ArrayList<>();
        }
        List<String> emails = new ArrayList<>();
        for (String s : raw.split(",")) {
            String e = s.trim();
            if (!e.isEmpty() && !emails.contains(e)) {
                emails.add(e);
            }
        }
        return emails;
    }

    /** 设置管理员通知邮箱列表（校验格式、去重、最多 10 个） */
    public void setNotifyEmails(List<String> emails) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (emails != null) {
            for (String e : emails) {
                if (e != null && !e.isBlank()) {
                    set.add(e.trim());
                }
            }
        }
        for (String e : set) {
            if (!e.matches(EMAIL_REGEX)) {
                throw new IllegalArgumentException("邮箱格式不正确: " + e);
            }
        }
        if (set.size() > MAX_EMAILS) {
            throw new IllegalArgumentException("最多只能绑定 " + MAX_EMAILS + " 个邮箱");
        }
        settingService.set(KEY_NOTIFY_EMAILS, String.join(",", set));
    }

    /** 异步通知所有管理员邮箱：有新教师注册待审核 */
    public void notifyTeacherRegisteredAsync(String username, String realName) {
        executor.submit(() -> {
            try {
                List<String> emails = getNotifyEmails();
                if (emails.isEmpty()) {
                    return;
                }
                if (!mailService.isEnabled()) {
                    log.info("邮件服务未配置，跳过教师注册通知（开发模式）: username={}", username);
                    return;
                }
                String subject = "【AI答疑】新教师注册通知";
                String text = "系统有新教师注册，请及时审核：\n\n工号：" + username
                        + "\n姓名：" + realName + "\n\n请登录管理后台处理。";
                for (String email : emails) {
                    mailService.sendMail(email, subject, text);
                }
            } catch (Exception e) {
                log.error("教师注册通知发送异常", e);
            }
        });
    }
}
