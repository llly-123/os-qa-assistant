package com.xidian.osqa.controller;

import com.xidian.osqa.common.Result;
import com.xidian.osqa.config.AiModelProvider;
import com.xidian.osqa.service.SystemSettingService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class SettingController {

    private final SystemSettingService settingService;
    private final AiModelProvider aiModelProvider;

    public SettingController(SystemSettingService settingService, AiModelProvider aiModelProvider) {
        this.settingService = settingService;
        this.aiModelProvider = aiModelProvider;
    }

    @GetMapping("/admin/settings")
    public Result<?> getAll() {
        return Result.success(settingService.getAllMasked());
    }

    @PutMapping("/admin/settings")
    public Result<?> update(@RequestBody Map<String, String> body) {
        settingService.setAll(body);
        return Result.success();
    }

    /** 清空敏感密钥（如 AI Key、短信 Secret） */
    @DeleteMapping("/admin/settings/{key}")
    public Result<?> clear(@PathVariable String key) {
        if (!settingService.isSensitiveKey(key)) {
            return Result.error(400, "该配置项不允许清空");
        }
        settingService.remove(key);
        return Result.success();
    }

    /** 测试 AI 接口配置是否有效（用传入值做一次真实调用，不落库） */
    @PostMapping("/admin/settings/test-ai")
    public Result<?> testAi(@RequestBody Map<String, String> body) {
        return Result.success(aiModelProvider.test(
                body.get("ai_api_key"),
                body.get("ai_base_url"),
                body.get("ai_model_name")));
    }
}
