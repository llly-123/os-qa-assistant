package com.xidian.osqa.service;

import com.xidian.osqa.common.CryptoUtil;
import com.xidian.osqa.entity.SystemSetting;
import com.xidian.osqa.mapper.SystemSettingMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SystemSettingService {

    /** 需要加密存储的敏感键 */
    private static final Set<String> SENSITIVE_KEYS = Set.of("ai_api_key", "sms_access_key_secret", "mail_password");

    private final SystemSettingMapper settingMapper;
    private final CryptoUtil cryptoUtil;

    public SystemSettingService(SystemSettingMapper settingMapper, CryptoUtil cryptoUtil) {
        this.settingMapper = settingMapper;
        this.cryptoUtil = cryptoUtil;
    }

    /** 读取（敏感字段自动解密，返回明文供内部使用） */
    public String get(String key) {
        SystemSetting s = settingMapper.selectById(key);
        if (s == null) {
            return null;
        }
        String v = s.getSettingValue();
        return SENSITIVE_KEYS.contains(key) ? cryptoUtil.decrypt(v) : v;
    }

    public String getOrDefault(String key, String defaultValue) {
        String v = get(key);
        return (v == null || v.isBlank()) ? defaultValue : v;
    }

    /** 写入（敏感字段自动加密存储；敏感字段传空/脱敏值表示不修改） */
    public void set(String key, String value) {
        if (SENSITIVE_KEYS.contains(key)
                && (value == null || value.isBlank() || CryptoUtil.isMasked(value))) {
            return;
        }
        String stored = SENSITIVE_KEYS.contains(key) ? cryptoUtil.encrypt(value) : value;

        SystemSetting s = settingMapper.selectById(key);
        if (s == null) {
            s = new SystemSetting();
            s.setSettingKey(key);
            s.setSettingValue(stored);
            s.setUpdateTime(LocalDateTime.now());
            settingMapper.insert(s);
        } else {
            s.setSettingValue(stored);
            s.setUpdateTime(LocalDateTime.now());
            settingMapper.updateById(s);
        }
    }

    /** 全量写入（敏感字段沿用 set 的脱敏/加密规则） */
    public void setAll(Map<String, String> entries) {
        if (entries == null) {
            return;
        }
        for (Map.Entry<String, String> e : entries.entrySet()) {
            set(e.getKey(), e.getValue());
        }
    }

    /** 明文视图（敏感字段解密后返回，供内部使用） */
    public Map<String, String> getAll() {
        List<SystemSetting> all = settingMapper.selectList(null);
        Map<String, String> map = new LinkedHashMap<>();
        for (SystemSetting s : all) {
            String v = s.getSettingValue() != null ? s.getSettingValue() : "";
            map.put(s.getSettingKey(), SENSITIVE_KEYS.contains(s.getSettingKey()) ? cryptoUtil.decrypt(v) : v);
        }
        return map;
    }

    /** 脱敏视图（敏感字段打码，供前端回显） */
    public Map<String, String> getAllMasked() {
        Map<String, String> all = getAll();
        Map<String, String> masked = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : all.entrySet()) {
            masked.put(e.getKey(), SENSITIVE_KEYS.contains(e.getKey()) ? CryptoUtil.mask(e.getValue()) : e.getValue());
        }
        return masked;
    }

    /** 是否为敏感键（敏感键允许被清空） */
    public boolean isSensitiveKey(String key) {
        return SENSITIVE_KEYS.contains(key);
    }

    /** 删除指定配置项（仅用于清空敏感密钥） */
    public void remove(String key) {
        settingMapper.deleteById(key);
    }
}
