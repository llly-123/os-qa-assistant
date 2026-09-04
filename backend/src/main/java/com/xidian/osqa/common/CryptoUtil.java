package com.xidian.osqa.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 敏感字段加解密工具（AES/GCM）。
 *
 * 用于对数据库中的密钥类字段（AI API Key、短信 AccessKey Secret、教师 API Key 等）加密存储。
 * 密钥来自环境变量 APP_ENCRYPT_KEY；未设置时使用内置开发密钥（仅用于本地开发，勿用于生产）。
 *
 * 密文统一带 "enc:" 前缀，便于兼容历史明文数据：解密时无前缀的值原样返回。
 */
@Component
public class CryptoUtil {

    private static final String PREFIX = "enc:";
    private static final String MASK = "****";

    private final SecretKey secretKey;

    public CryptoUtil(@Value("${app.encrypt-key:}") String envKey) {
        String raw = (envKey == null || envKey.isBlank()) ? "os-qa-assistant-dev-default-key" : envKey;
        this.secretKey = deriveKey(raw);
    }

    private SecretKey deriveKey(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return new SecretKeySpec(digest.digest(raw.getBytes(StandardCharsets.UTF_8)), "AES");
        } catch (Exception e) {
            throw new IllegalStateException("无法初始化加密密钥", e);
        }
    }

    /** 加密，返回带 "enc:" 前缀的密文；空值原样返回 */
    public String encrypt(String plain) {
        if (plain == null || plain.isBlank()) {
            return plain;
        }
        try {
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("加密失败", e);
        }
    }

    /** 解密；无前缀或解密失败时原样返回，避免历史明文/密钥变更导致崩溃 */
    public String decrypt(String stored) {
        if (stored == null || stored.isBlank() || !stored.startsWith(PREFIX)) {
            return stored;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            byte[] iv = new byte[12];
            byte[] encrypted = new byte[combined.length - 12];
            System.arraycopy(combined, 0, iv, 0, 12);
            System.arraycopy(combined, 12, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return stored;
        }
    }

    /** 脱敏：仅显示末 4 位，其余打码 */
    public static String mask(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (value.length() <= 4) {
            return MASK;
        }
        return MASK + value.substring(value.length() - 4);
    }

    /** 是否为脱敏值（前端未修改、原样提交的占位值） */
    public static boolean isMasked(String value) {
        return value != null && value.startsWith(MASK);
    }
}
