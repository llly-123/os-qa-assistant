package com.xidian.osqa.service;

import com.xidian.osqa.common.CryptoUtil;
import com.xidian.osqa.entity.TeacherApiConfig;
import com.xidian.osqa.mapper.TeacherApiConfigMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 教师级 API 配置服务。
 *
 * 优先级：教师自己的配置 > 管理员的系统默认配置。
 * 管理员可通过设置教师的体验时间段来开放使用管理员 API 的权限，
 * 在体验时间段内教师未配置自己的 API 时自动回退到管理员的配置。
 *
 * apiKey 加密存储于 teacher_api_config 表。
 */
@Service
public class TeacherApiConfigService {

    private final TeacherApiConfigMapper configMapper;
    private final SystemSettingService settingService;
    private final CryptoUtil cryptoUtil;

    public TeacherApiConfigService(TeacherApiConfigMapper configMapper,
                                   SystemSettingService settingService,
                                   CryptoUtil cryptoUtil) {
        this.configMapper = configMapper;
        this.settingService = settingService;
        this.cryptoUtil = cryptoUtil;
    }

    /** 获取教师的 API 配置（apiKey 解密为明文，仅教师本人/管理员可调） */
    public TeacherApiConfig getByTeacherId(Long teacherId) {
        TeacherApiConfig config = configMapper.selectById(teacherId);
        if (config != null && config.getApiKey() != null) {
            config.setApiKey(cryptoUtil.decrypt(config.getApiKey()));
        }
        return config;
    }

    /** 保存或更新教师的 API 配置（apiKey 空/脱敏值表示不修改，其余加密存储） */
    public void save(Long teacherId, String apiKey, String baseUrl, String modelName) {
        TeacherApiConfig existing = configMapper.selectById(teacherId);

        String storedApiKey;
        if (apiKey == null || apiKey.isBlank() || CryptoUtil.isMasked(apiKey)) {
            // 不修改 apiKey，保留原值（库中已是密文）
            storedApiKey = existing != null ? existing.getApiKey() : null;
        } else {
            storedApiKey = cryptoUtil.encrypt(apiKey);
        }

        if (existing == null) {
            existing = new TeacherApiConfig();
            existing.setTeacherId(teacherId);
            existing.setApiKey(storedApiKey);
            existing.setBaseUrl(baseUrl);
            existing.setModelName(modelName);
            existing.setUpdateTime(LocalDateTime.now());
            configMapper.insert(existing);
        } else {
            existing.setApiKey(storedApiKey);
            existing.setBaseUrl(baseUrl);
            existing.setModelName(modelName);
            existing.setUpdateTime(LocalDateTime.now());
            configMapper.updateById(existing);
        }
    }

    /** 清除教师的 API 配置，回退到管理员默认 */
    public void clear(Long teacherId) {
        configMapper.deleteById(teacherId);
    }

    /**
     * 解析教师实际使用的 API 配置。
     *
     * 规则：
     * 1. 教师已配置自己的 API（apiKey 非空）→ 使用教师自己的
     * 2. 教师未配置，但在体验时间段内 → 使用管理员的默认配置
     * 3. 教师未配置，且不在体验时间段内 → 返回 null（由调用方处理）
     *
     * @param teacherId      教师ID
     * @param inTrialPeriod  是否在体验时间段内
     * @return 包含 apiKey/baseUrl/modelName 的 Map，或 null
     */
    public Map<String, String> resolveConfig(Long teacherId, boolean inTrialPeriod) {
        // 1. 优先使用教师自己的配置（getByTeacherId 已解密）
        TeacherApiConfig teacherConfig = teacherId != null ? getByTeacherId(teacherId) : null;
        if (teacherConfig != null && teacherConfig.getApiKey() != null && !teacherConfig.getApiKey().isBlank()) {
            Map<String, String> config = new LinkedHashMap<>();
            config.put("apiKey", teacherConfig.getApiKey());
            config.put("baseUrl", teacherConfig.getBaseUrl());
            config.put("modelName", teacherConfig.getModelName());
            return config;
        }

        // 2. 教师未配置，在体验时间段内 → 回退到管理员默认
        if (inTrialPeriod) {
            return getAdminConfig();
        }

        // 3. 未配置且不在体验期 → 无可用配置
        return null;
    }

    /** 获取管理员的系统默认 API 配置（apiKey 已由 SystemSettingService 解密） */
    public Map<String, String> getAdminConfig() {
        Map<String, String> config = new LinkedHashMap<>();
        config.put("apiKey", settingService.getOrDefault("ai_api_key", ""));
        config.put("baseUrl", settingService.getOrDefault("ai_base_url", ""));
        config.put("modelName", settingService.getOrDefault("ai_model_name", ""));
        return config;
    }
}
