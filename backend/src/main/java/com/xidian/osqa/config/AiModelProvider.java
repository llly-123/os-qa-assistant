package com.xidian.osqa.config;

import com.xidian.osqa.service.SystemSettingService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 动态 AI 模型提供者。
 *
 * 优先读取数据库 system_setting 中的配置（教师端可在界面修改），
 * 未配置时回退到 application.yml 的默认值。配置变化时自动重建模型实例。
 * 统一使用 OpenAI 兼容协议（DeepSeek 即 OpenAI 兼容）。
 */
@Component
public class AiModelProvider {

    private final SystemSettingService settingService;
    private final String defaultApiKey;
    private final String defaultBaseUrl;
    private final String defaultModelName;

    private volatile ChatLanguageModel cachedModel;
    private volatile String cachedSignature;

    public AiModelProvider(SystemSettingService settingService,
                           @Value("${langchain4j.deepseek.api-key}") String defaultApiKey,
                           @Value("${langchain4j.deepseek.base-url}") String defaultBaseUrl,
                           @Value("${langchain4j.deepseek.model-name}") String defaultModelName) {
        this.settingService = settingService;
        this.defaultApiKey = defaultApiKey;
        this.defaultBaseUrl = defaultBaseUrl;
        this.defaultModelName = defaultModelName;
    }

    /** 获取当前有效的模型实例；配置变更时按需重建（双重检查锁） */
    public ChatLanguageModel getModel() {
        String apiKey = settingService.getOrDefault("ai_api_key", defaultApiKey);
        String baseUrl = settingService.getOrDefault("ai_base_url", defaultBaseUrl);
        String modelName = settingService.getOrDefault("ai_model_name", defaultModelName);
        String signature = apiKey + "\n" + baseUrl + "\n" + modelName;

        ChatLanguageModel model = cachedModel;
        if (model == null || !signature.equals(cachedSignature)) {
            synchronized (this) {
                model = cachedModel;
                if (model == null || !signature.equals(cachedSignature)) {
                    model = OpenAiChatModel.builder()
                            .apiKey(apiKey)
                            .baseUrl(baseUrl)
                            .modelName(modelName)
                            .timeout(Duration.ofSeconds(120))
                            .build();
                    cachedModel = model;
                    cachedSignature = signature;
                }
            }
        }
        return cachedModel;
    }

    /** 用指定配置发起一次真实调用，验证 API 是否有效（不修改已保存配置） */
    public Map<String, Object> test(String apiKey, String baseUrl, String modelName) {
        String key = (apiKey == null || apiKey.isBlank()) ? defaultApiKey : apiKey.trim();
        String url = (baseUrl == null || baseUrl.isBlank()) ? defaultBaseUrl : baseUrl.trim();
        String model = (modelName == null || modelName.isBlank()) ? defaultModelName : modelName.trim();

        Map<String, Object> result = new LinkedHashMap<>();
        try {
            ChatLanguageModel testModel = OpenAiChatModel.builder()
                    .apiKey(key)
                    .baseUrl(url)
                    .modelName(model)
                    .timeout(Duration.ofSeconds(20))
                    .build();
            Response<AiMessage> response = testModel.generate(new UserMessage("请回复：连接成功"));
            String reply = response != null && response.content() != null ? response.content().text() : "";
            result.put("success", true);
            result.put("message", "连接成功");
            result.put("reply", reply);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "连接失败：" + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }
        return result;
    }
}
