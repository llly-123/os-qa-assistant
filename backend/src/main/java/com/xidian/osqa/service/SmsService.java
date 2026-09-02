package com.xidian.osqa.service;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 短信发送服务（动态可配置）。
 *
 * 通过环境变量配置阿里云短信：accessKeyId / accessKeySecret / signName / templateCode
 * 四项齐全后 isEnabled() 才返回 true 并真正发送短信；否则调用方回退到开发模式（日志打印验证码）。
 * 这样个人部署（未完成企业认证）时无需任何短信配置即可跑通流程，拿到密钥后填环境变量即自动切换为真短信。
 */
@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    @Value("${sms.access-key-id:}")
    private String accessKeyId;

    @Value("${sms.access-key-secret:}")
    private String accessKeySecret;

    @Value("${sms.sign-name:}")
    private String signName;

    @Value("${sms.template-code:}")
    private String templateCode;

    /** 是否已配置真实短信服务（四项齐全才启用） */
    public boolean isEnabled() {
        return isNotBlank(accessKeyId) && isNotBlank(accessKeySecret)
                && isNotBlank(signName) && isNotBlank(templateCode);
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
