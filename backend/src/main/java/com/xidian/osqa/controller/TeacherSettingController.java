package com.xidian.osqa.controller;

import com.xidian.osqa.common.CryptoUtil;
import com.xidian.osqa.common.Result;
import com.xidian.osqa.config.AiModelProvider;
import com.xidian.osqa.entity.TeacherApiConfig;
import com.xidian.osqa.entity.User;
import com.xidian.osqa.mapper.UserMapper;
import com.xidian.osqa.service.TeacherApiConfigService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 教师级 API 配置接口。
 * 教师可配置自己的 AI API；未配置时在体验时间段内回退到管理员默认配置。
 */
@RestController
@RequestMapping("/api/teacher")
public class TeacherSettingController {

    private final TeacherApiConfigService teacherApiConfigService;
    private final AiModelProvider aiModelProvider;
    private final UserMapper userMapper;

    public TeacherSettingController(TeacherApiConfigService teacherApiConfigService, AiModelProvider aiModelProvider, UserMapper userMapper) {
        this.teacherApiConfigService = teacherApiConfigService;
        this.aiModelProvider = aiModelProvider;
        this.userMapper = userMapper;
    }

    /** 获取当前教师的 API 配置及体验状态 */
    @GetMapping("/api-config")
    public Result<?> getApiConfig(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");

        // 管理员可查看任意教师的配置（通过 query 参数 teacherId）
        Long targetTeacherId = userId;
        if ("SUPER_ADMIN".equals(role)) {
            String teacherIdParam = request.getParameter("teacherId");
            if (teacherIdParam != null) {
                targetTeacherId = Long.valueOf(teacherIdParam);
            }
        }

        TeacherApiConfig config = teacherApiConfigService.getByTeacherId(targetTeacherId);
        boolean usingOwnConfig = config != null && config.getApiKey() != null && !config.getApiKey().isBlank();

        // 查询教师的体验时间段
        User teacher = userMapper.selectById(targetTeacherId);
        boolean inTrial = false;
        String trialStartTime = null;
        String trialEndTime = null;
        if (teacher != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime trialStart = teacher.getTrialStartTime();
            LocalDateTime trialEnd = teacher.getTrialEndTime();
            trialStartTime = trialStart != null ? trialStart.toString() : null;
            trialEndTime = trialEnd != null ? trialEnd.toString() : null;
            inTrial = trialStart != null && trialEnd != null && !now.isBefore(trialStart) && !now.isAfter(trialEnd);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hasOwnConfig", usingOwnConfig);
        result.put("inTrialPeriod", inTrial);
        result.put("trialStartTime", trialStartTime);
        result.put("trialEndTime", trialEndTime);
        result.put("apiKey", usingOwnConfig ? CryptoUtil.mask(config.getApiKey()) : "");
        result.put("baseUrl", config != null ? config.getBaseUrl() : "");
        result.put("modelName", config != null ? config.getModelName() : "");

        // 管理员默认配置（仅展示给教师用于参考，不暴露 apiKey 明文）
        Map<String, String> adminConfig = teacherApiConfigService.getAdminConfig();
        result.put("adminBaseUrl", adminConfig.get("baseUrl"));
        result.put("adminModelName", adminConfig.get("modelName"));
        result.put("adminApiKeySet", adminConfig.get("apiKey") != null && !adminConfig.get("apiKey").isBlank());

        return Result.success(result);
    }

    /** 保存当前教师的 API 配置 */
    @PutMapping("/api-config")
    public Result<?> saveApiConfig(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");

        // 管理员可代为配置（通过 body 中 teacherId）
        Long targetTeacherId = userId;
        if ("SUPER_ADMIN".equals(role) && body.get("teacherId") != null) {
            targetTeacherId = Long.valueOf(body.get("teacherId"));
        }

        teacherApiConfigService.save(targetTeacherId, body.get("apiKey"), body.get("baseUrl"), body.get("modelName"));
        aiModelProvider.evictTeacherCache(targetTeacherId);
        return Result.success();
    }

    /** 清除当前教师的 API 配置，回退到管理员默认 */
    @DeleteMapping("/api-config")
    public Result<?> clearApiConfig(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        teacherApiConfigService.clear(userId);
        aiModelProvider.evictTeacherCache(userId);
        return Result.success();
    }

    /** 测试教师 API 配置是否有效（不落库） */
    @PostMapping("/api-config/test")
    public Result<?> testApiConfig(@RequestBody Map<String, String> body) {
        return Result.success(aiModelProvider.test(body.get("apiKey"), body.get("baseUrl"), body.get("modelName")));
    }
}
