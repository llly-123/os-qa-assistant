package com.xidian.osqa.config;

import com.xidian.osqa.service.SystemSettingService;
import com.xidian.osqa.service.TeacherApiConfigService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态 AI 模型提供者。
 *
 * 支持两级 API 配置：
 * 1. 管理员级（system_setting 表中的 ai_api_key / ai_base_url / ai_model_name）
 * 2. 教师级（teacher_api_config 表，教师自己的 API 配置）
 *
 * 教师未配置自己的 API 时，在管理员开放的体验时间段内回退到管理员的配置。
 * 管理员未放开权限（不在体验时间段内）且教师未配置自己的 API 时，无法使用 AI。
 */
@Component
public class AiModelProvider {

    private final SystemSettingService settingService;
    private final TeacherApiConfigService teacherApiConfigService;

    /** 管理员级模型缓存 */
    private volatile ChatLanguageModel cachedAdminModel;
    private volatile String cachedAdminSignature;

    /** 教师级模型缓存（按 teacherId 隔离） */
    private final Map<Long, ModelCache> teacherModelCache = new ConcurrentHashMap<>();

    private record ModelCache(ChatLanguageModel model, String signature) {}

    public AiModelProvider(SystemSettingService settingService,
                           TeacherApiConfigService teacherApiConfigService) {
        this.settingService = settingService;
        this.teacherApiConfigService = teacherApiConfigService;
    }

    /**
     * 获取管理员级模型实例（向后兼容，无教师上下文时使用）。
     * 配置变更时按需重建（双重检查锁）。
     */
    public ChatLanguageModel getModel() {
        String apiKey = settingService.get("ai_api_key");
        String baseUrl = settingService.get("ai_base_url");
        String modelName = settingService.get("ai_model_name");

        // 无 API Key 时返回 null，由调用方给出友好提示
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }

        String signature = apiKey + "\n" + baseUrl + "\n" + modelName;

        ChatLanguageModel model = cachedAdminModel;
        if (model == null || !signature.equals(cachedAdminSignature)) {
            synchronized (this) {
                model = cachedAdminModel;
                if (model == null || !signature.equals(cachedAdminSignature)) {
                    model = OpenAiChatModel.builder()
                            .apiKey(apiKey)
                            .baseUrl(baseUrl)
                            .modelName(modelName)
                            .timeout(Duration.ofSeconds(120))
                            .build();
                    cachedAdminModel = model;
                    cachedAdminSignature = signature;
                }
            }
        }
        return cachedAdminModel;
    }

    /**
     * 获取教师级模型实例。
     *
     * 优先使用教师自己的 API 配置；未配置时在体验时间段内回退到管理员配置。
     *
     * @param teacherId      教师ID
     * @param inTrialPeriod  是否在体验时间段内
     * @return 模型实例，或 null（未配置且不在体验期）
     */
    public ChatLanguageModel getModel(Long teacherId, boolean inTrialPeriod) {
        Map<String, String> config = teacherApiConfigService.resolveConfig(teacherId, inTrialPeriod);
        if (config == null || config.get("apiKey") == null || config.get("apiKey").isBlank()) {
            return null;
        }

        String apiKey = config.get("apiKey");
        String baseUrl = config.get("baseUrl");
        String modelName = config.get("modelName");
        String signature = apiKey + "\n" + baseUrl + "\n" + modelName;

        // 体验期使用管理员配置时走管理员缓存
        boolean usingAdminConfig = inTrialPeriod && (teacherId == null ||
                teacherApiConfigService.getByTeacherId(teacherId) == null ||
                teacherApiConfigService.getByTeacherId(teacherId).getApiKey() == null ||
                teacherApiConfigService.getByTeacherId(teacherId).getApiKey().isBlank());

        if (usingAdminConfig) {
            return getModel();
        }

        // 教师自己的配置，按 teacherId 缓存
        ModelCache cache = teacherModelCache.get(teacherId);
        if (cache != null && cache.signature().equals(signature)) {
            return cache.model();
        }

        synchronized (teacherModelCache) {
            cache = teacherModelCache.get(teacherId);
            if (cache == null || !cache.signature().equals(signature)) {
                ChatLanguageModel model = OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        .timeout(Duration.ofSeconds(120))
                        .build();
                cache = new ModelCache(model, signature);
                teacherModelCache.put(teacherId, cache);
            }
        }
        return cache.model();
    }

    /** 清除指定教师的模型缓存（教师修改配置后调用） */
    public void evictTeacherCache(Long teacherId) {
        if (teacherId != null) {
            teacherModelCache.remove(teacherId);
        }
    }

    /** 用指定配置发起一次真实调用，验证 API 是否有效（不修改已保存配置） */
    public Map<String, Object> test(String apiKey, String baseUrl, String modelName) {
        String key = apiKey == null ? "" : apiKey.trim();
        String url = baseUrl == null ? "" : baseUrl.trim();
        String model = modelName == null ? "" : modelName.trim();

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
