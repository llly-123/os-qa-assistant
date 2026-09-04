package com.xidian.osqa.service;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 短信发送服务（动态可配置）。
 *
 * 阿里云短信配置（accessKeyId / accessKeySecret / signName / templateCode）
 * 由管理员在系统设置页面维护，存储于 system_setting 表。
 * 四项齐全后 isEnabled() 才返回 true 并真正发送短信；否则调用方回退到开发模式（日志打印验证码）。
 * 这样个人部署（未完成企业认证）时无需任何短信配置即可跑通流程，管理员填齐后自动切换为真短信。
 */
@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    private static final String KEY_ACCESS_KEY_ID = "sms_access_key_id";
    private static final String KEY_ACCESS_KEY_SECRET = "sms_access_key_secret";
    private static final String KEY_SIGN_NAME = "sms_sign_name";
    private static final String KEY_TEMPLATE_CODE = "sms_template_code";

    private final SystemSettingService settingService;

    public SmsService(SystemSettingService settingService) {
        this.settingService = settingService;
    }

    /** 是否已配置真实短信服务（四项齐全才启用） */
    public boolean isEnabled() {
        return isNotBlank(settingService.get(KEY_ACCESS_KEY_ID))
                && isNotBlank(settingService.get(KEY_ACCESS_KEY_SECRET))
                && isNotBlank(settingService.get(KEY_SIGN_NAME))
                && isNotBlank(settingService.get(KEY_TEMPLATE_CODE));
    }

    /**
     * 发送验证码短信（阿里云短信）。
     * 调用前需先通过 isEnabled() 判断是否已配置；未配置时本方法不会真正发送。
     *
     * @return 是否发送成功
     */
    public boolean sendVerifyCode(String phone, String code) {
        if (!isEnabled()) {
            return false;
        }
        try {
            String accessKeyId = settingService.get(KEY_ACCESS_KEY_ID);
            String accessKeySecret = settingService.get(KEY_ACCESS_KEY_SECRET);
            String signName = settingService.get(KEY_SIGN_NAME);
            String templateCode = settingService.get(KEY_TEMPLATE_CODE);

            Config config = new Config()
                    .setAccessKeyId(accessKeyId)
                    .setAccessKeySecret(accessKeySecret);
            config.endpoint = "dysmsapi.aliyuncs.com";
            Client client = new Client(config);

            SendSmsRequest request = new SendSmsRequest()
                    .setPhoneNumbers(phone)
                    .setSignName(signName)
                    .setTemplateCode(templateCode)
                    .setTemplateParam("{\"code\":\"" + code + "\"}");

            SendSmsResponse response = client.sendSms(request);
            String respCode = response.getBody() != null ? response.getBody().getCode() : null;
            if ("OK".equals(respCode)) {
                log.info("短信验证码已发送: phone={}", phone);
                return true;
            }
            log.error("短信发送失败: phone={}, respCode={}, message={}", phone, respCode,
                    response.getBody() != null ? response.getBody().getMessage() : "");
            return false;
        } catch (Exception e) {
            log.error("短信发送异常: phone={}, error={}", phone, e.getMessage(), e);
            return false;
        }
    }

    private boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }
}
