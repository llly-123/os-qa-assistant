package com.xidian.osqa.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.util.Properties;

/**
 * 邮件发送服务（动态可配置）。
 *
 * SMTP 配置（host / port / username / password授权码）由管理员在系统设置页面维护，
 * 存储于 system_setting 表，其中 password（授权码）加密存储。
 * 四项齐全后 isEnabled() 才返回 true 并真正发送邮件；否则调用方回退到开发模式（日志打印验证码）。
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private static final String KEY_HOST = "mail_host";
    private static final String KEY_PORT = "mail_port";
    private static final String KEY_USERNAME = "mail_username";
    private static final String KEY_PASSWORD = "mail_password";

    private final SystemSettingService settingService;

    public MailService(SystemSettingService settingService) {
        this.settingService = settingService;
    }

    /** 是否已配置真实邮件服务（四项齐全才启用） */
    public boolean isEnabled() {
        return isNotBlank(settingService.get(KEY_HOST))
                && isNotBlank(settingService.get(KEY_PORT))
                && isNotBlank(settingService.get(KEY_USERNAME))
                && isNotBlank(settingService.get(KEY_PASSWORD));
    }

    /**
     * 发送验证码邮件。
     * 调用前需先通过 isEnabled() 判断是否已配置；未配置时本方法不会真正发送。
     *
     * @return 是否发送成功
     */
    public boolean sendVerifyCode(String to, String code) {
        if (!isEnabled()) {
            return false;
        }
        try {
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(settingService.get(KEY_HOST));
            int port = Integer.parseInt(settingService.get(KEY_PORT).trim());
            sender.setPort(port);
            sender.setUsername(settingService.get(KEY_USERNAME));
            sender.setPassword(settingService.get(KEY_PASSWORD));
            sender.setDefaultEncoding("UTF-8");

            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            if (port == 465) {
                // 465 端口走 SSL（SMTPS）
                props.put("mail.smtp.ssl.enable", "true");
            } else {
                // 587 等端口走 STARTTLS
                props.put("mail.smtp.starttls.enable", "true");
            }
            sender.setJavaMailProperties(props);

            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(settingService.get(KEY_USERNAME));
            msg.setTo(to);
            msg.setSubject("【AI答疑】邮箱验证码");
            msg.setText("您的验证码是：" + code + "，5分钟内有效。如非本人操作请忽略。");
            sender.send(msg);

            log.info("验证码邮件已发送: to={}", to);
            return true;
        } catch (Exception e) {
            log.error("邮件发送失败: to={}, error={}", to, e.getMessage(), e);
            return false;
        }
    }

    private boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }
}
