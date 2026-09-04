package com.xidian.osqa.service;

import com.xidian.osqa.common.AnswerSanitizer;
import com.xidian.osqa.config.AiModelProvider;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final AiModelProvider aiModelProvider;
    private final KnowledgeEmbeddingService embeddingService;
    private final WebSearchService webSearchService;
    private final SystemSettingService settingService;

    // 缓存最近一次联网搜索的结果，供getCitation复用
    private static final ThreadLocal<List<WebSearchService.SearchResult>> cachedSearchResults = new ThreadLocal<>();

    // 缓存最近一次教材检索结果，供getCitation复用，避免同一问题检索两次
    private static final ThreadLocal<RetrievalCache> cachedRetrieval = new ThreadLocal<>();

    private static class RetrievalCache {
        final String question;
        final Long kbId;
        final List<String> chunks;
        RetrievalCache(String question, Long kbId, List<String> chunks) {
            this.question = question;
            this.kbId = kbId;
            this.chunks = chunks;
        }
        boolean matches(String q, Long k) {
            return java.util.Objects.equals(this.question, q) && java.util.Objects.equals(this.kbId, k);
        }
    }

    private String courseName() {
        return settingService.getOrDefault("course_name", "本课程");
    }

    private String schoolName() {
        return settingService.getOrDefault("school_name", "");
    }

    /** 拼装系统提示词，使用可配置的课程名/学校名 */
    private String buildSystemPrompt(boolean webSearch) {
        String course = courseName();
        String school = schoolName();
        String owner = (school == null || school.isBlank())
                ? "《" + course + "》"
                : school + "《" + course + "》";
        // 输出规范硬性约束（所有模式通用），借鉴 DeepSeek：只输出成品，不输出思考过程
        String outputConstraint = """
                【输出规范·硬性约束】
                A. 禁止输出任何 AI 自身的思考、推演、验算、疑问、矛盾拆解、推测类文本，只输出最终成品答案。
                B. 禁止使用以下句式开头或出现：重新梳理、注意、片段给出、可是根据、可能、笔误。
                C. 正文仅保留：定义、数据结构、伪代码、表格、标准计算步骤、客观总结。
                D. 若素材之间存在冲突，仅在文末用单行备注说明（如"注：某处记载为X，此处采用Y"），正文不展开分析。
                E. 不要展示草稿、中间步骤、自我纠错过程。""";
        if (webSearch) {
            return """
                    你是%s课程的AI答疑助教。学生开启了联网搜索功能。
                    1. 首先基于课程知识回答问题。
                    2. 如果课程知识不足以完整回答，请利用你的专业知识进行补充，确保回答完整。
                    3. 对于课程中有的内容，标注"📚 课程内容"。
                    4. 对于你补充的网络/通用知识，标注"🌐 补充知识"。
                    5. 若问题与%s课程完全无关，请明确回复"该问题与当前课程无关，暂不回答"，不要强行作答。
                    6. 回答格式清晰，使用Markdown格式，包含代码示例和公式时使用代码块。
                    7. 确保回答完整、准确，不要只说"未找到"就结束，必须给出完整的知识点解答。
                    8. 引用课程内容时，用"第X章"或"第X节"来引用，例如"根据第3章中的描述"。
                    9. 在回答最末尾，用Markdown链接格式列出1-3个参考来源。注意：
                       - 优先使用百度百科、CSDN、博客园、菜鸟教程等国内可稳定访问的网站
                       - 不要使用维基百科（国内无法访问）
                       - 不要使用知乎（有反爬机制，经常加载失败）
                       - 链接格式：[标题](URL)
                    10. 不要提及"你提供的教材""检索到的片段"等内部技术细节，对学生来说你就是一位知识渊博的助教。
                    11. 不要在回答中单独列出"📚 教材来源"或"🌐 网络搜索来源"等分区标题，来源分区由系统自动整理。

                    %s
                    """.formatted(owner, course, course, outputConstraint);
        }
        return """
                你是%s课程的AI答疑助教。你的职责是：
                1. 基于课程知识回答问题，确保答案准确无误。
                2. 如果课程中没有相关内容，请明确告知学生"课程中未找到相关内容"，并建议查阅其他资料或咨询老师。
                3. 回答时尽量使用标准术语和学术表述，保持学术严谨性。
                4. 如果提供了网络搜索结果，可以补充说明，但需明确标注来源。
                5. 若问题与%s课程完全无关，请明确回复"该问题与当前课程无关，暂不回答"，不要强行作答。
                6. 回答格式清晰，使用Markdown格式，包含代码示例和公式时使用代码块。
                7. 引用课程内容时，用"第X章"或"第X节"来引用，例如"根据第3章中的描述"。
                8. 不要提及"你提供的教材""检索到的片段"等内部技术细节，对学生来说你就是一位知识渊博的助教。

                %s
                """.formatted(owner, course, course, outputConstraint);
    }

    public RagService(AiModelProvider aiModelProvider, KnowledgeEmbeddingService embeddingService, WebSearchService webSearchService, SystemSettingService settingService) {
        this.aiModelProvider = aiModelProvider;
        this.embeddingService = embeddingService;
        this.webSearchService = webSearchService;
        this.settingService = settingService;
    }

    public String answer(String question, boolean webSearch) {
        return answer(question, webSearch, null);
    }

    public String answer(String question, boolean webSearch, Long kbId) {
        return answer(question, webSearch, kbId, null, false);
    }

    /**
     * 回答学生问题。
     * @param teacherId     会话所属班级的教师ID（用于解析教师级API配置）
     * @param inTrialPeriod 教师是否在体验时间段内
     */
    public String answer(String question, boolean webSearch, Long kbId, Long teacherId, boolean inTrialPeriod) {
        try {
            // 清除旧的缓存
            cachedSearchResults.remove();
            cachedRetrieval.remove();

            String course = courseName();

            // 如果开启联网搜索，优先使用网络搜索（带超时保护，避免网络不通时阻塞整个请求）
            if (webSearch) {
                log.info("开启联网搜索模式");
                List<WebSearchService.SearchResult> searchResults = new ArrayList<>();
                try {
                    searchResults = java.util.concurrent.CompletableFuture
                            .supplyAsync(() -> webSearchService.search(question, course))
                            .get(10, java.util.concurrent.TimeUnit.SECONDS);
                } catch (java.util.concurrent.TimeoutException e) {
                    log.warn("联网搜索超时(10s)，降级为纯教材回答");
                } catch (Exception e) {
                    log.warn("联网搜索失败，降级为纯教材回答: {}", e.getMessage());
                }
                // 缓存搜索结果供getCitation使用
                cachedSearchResults.set(searchResults);
                log.info("联网搜索结果数量: {}", searchResults.size());

                if (!searchResults.isEmpty()) {
                    try {
                        final List<WebSearchService.SearchResult> finalResults = searchResults;
                        String aiAnswer = java.util.concurrent.CompletableFuture
                                .supplyAsync(() -> webSearchService.summarizeWithAI(finalResults, question))
                                .get(30, java.util.concurrent.TimeUnit.SECONDS);
                        if (aiAnswer != null) {
                            return AnswerSanitizer.sanitize(aiAnswer);
                        }
                    } catch (Exception e) {
                        log.warn("AI整理搜索结果超时，降级为教材回答: {}", e.getMessage());
                    }
                }
            }

            // 教材知识库检索（按班级知识库作用域），结果缓存供 getCitation 复用
            List<String> relevantChunks = embeddingService.retrieve(question, 8, kbId);
            cachedRetrieval.set(new RetrievalCache(question, kbId, relevantChunks));

            List<ChatMessage> messages = new ArrayList<>();

            // 联网搜索时使用更宽松的提示词，允许AI补充知识
            messages.add(new SystemMessage(buildSystemPrompt(webSearch)));

            StringBuilder contextBuilder = new StringBuilder();
            if (!relevantChunks.isEmpty()) {
                contextBuilder.append("以下是课程中检索到的相关内容：\n\n");
                for (int i = 0; i < relevantChunks.size(); i++) {
                    contextBuilder.append("【片段").append(i + 1).append("】\n")
                            .append(relevantChunks.get(i))
                            .append("\n\n");
                }
                if (webSearch) {
                    contextBuilder.append("请首先基于以上课程内容回答，如果内容不足以完整回答，请用你的专业知识补充完整。\n\n");
                } else {
                    contextBuilder.append("请基于以上课程内容回答学生的问题。如果以上内容不足以回答问题，请如实说明。\n\n");
                }
            } else {
                if (webSearch) {
                    contextBuilder.append("注意：课程知识库中未检索到与问题直接相关的内容。");
                    contextBuilder.append("请利用你的").append(course).append("专业知识给出完整、准确的回答，并标注为补充知识。\n\n");
                } else {
                    contextBuilder.append("注意：课程知识库中未检索到与问题直接相关的内容。");
                    contextBuilder.append("请基于").append(course).append("通用知识谨慎回答，并明确告知学生此回答未引用课程原文，建议开启联网搜索或咨询老师。\n\n");
                }
            }

            String userPrompt = contextBuilder + "学生问题：" + question;
            messages.add(new UserMessage(userPrompt));

            // 解析模型：
            // - 有教师上下文时，严格使用教师级配置（未配置且不在体验期则直接拒绝，不回退管理员）
            // - 无教师上下文时（如管理员自测），才使用管理员模型
            dev.langchain4j.model.chat.ChatLanguageModel model;
            if (teacherId != null) {
                model = aiModelProvider.getModel(teacherId, inTrialPeriod);
                if (model == null) {
                    return "抱歉，当前课程暂未开放AI答疑。请联系教师设置API接口，或等待管理员开放体验权限。";
                }
            } else {
                model = aiModelProvider.getModel();
                if (model == null) {
                    return "抱歉，AI服务暂未配置。请联系教师设置API接口，或等待管理员开放体验权限。";
                }
            }

            Response<AiMessage> response = model.generate(messages);
            String answer = AnswerSanitizer.sanitize(response.content().text());
            // 兜底：AI 返回空内容（如拒答无关问题被清洗后为空）时给出明确提示
            if (answer == null || answer.isBlank()) {
                answer = "该问题与当前课程无关，暂不回答。如需帮助，请提出与" + course + "课程相关的问题。";
            }
            return answer;
        } catch (Exception e) {
            log.error("AI回答失败", e);
            if (e.getMessage() != null && e.getMessage().contains("403")) {
                return "抱歉，AI服务余额不足，请联系管理员充值后使用。";
            }
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                return "抱歉，AI服务认证失败，请检查API Key配置。";
            }
            return "抱歉，AI回答时出现错误：" + e.getMessage();
        }
    }

    public String getCitation(String question, boolean webSearch) {
        return getCitation(question, webSearch, null);
    }

    public String getCitation(String question, boolean webSearch, Long kbId) {
        String course = courseName();
        // 联网搜索时返回教材来源 + 网络来源
        if (webSearch) {
            StringBuilder sb = new StringBuilder();

            // 统一来源列表
            sb.append("📖 参考来源：\n");

            // 教材来源（去重）
            List<String> relevantChunks = retrieveCached(question, kbId);
            if (!relevantChunks.isEmpty()) {
                Set<String> sources = new java.util.LinkedHashSet<>();
                for (String chunk : relevantChunks) {
                    if (chunk.contains("[来源:")) {
                        int start = chunk.indexOf("[来源:");
                        int end = chunk.indexOf("]", start);
                        if (end > start) {
                            sources.add(chunk.substring(start + 4, end));
                        }
                    }
                }
                for (String source : sources) {
                    sb.append("- 📚 ").append(source).append("\n");
                }
            }

            // 网络来源（具体文章链接 + 站内搜索链接）
            List<WebSearchService.SearchResult> searchResults = cachedSearchResults.get();

            String encodedQuery;
            try {
                encodedQuery = java.net.URLEncoder.encode(course + " " + question, java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception e) {
                encodedQuery = question;
            }
            String keywordQuery;
            try {
                keywordQuery = java.net.URLEncoder.encode(question, java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception e) {
                keywordQuery = question;
            }

            // 具体文章链接
            if (searchResults != null && !searchResults.isEmpty()) {
                for (int i = 0; i < searchResults.size(); i++) {
                    WebSearchService.SearchResult r = searchResults.get(i);
                    if (r.getUrl() != null && !r.getUrl().isEmpty()) {
                        String displayTitle = r.getTitle() != null && !r.getUrl().equals(r.getTitle())
                            ? r.getTitle()
                            : "来源" + (i + 1);
                        sb.append("- 🌐 [").append(displayTitle).append("](").append(r.getUrl()).append(")\n");
                    }
                }
            }

            // 站内搜索链接（备用）
            sb.append("- [🔍 Bing搜索](https://www.bing.com/search?q=").append(encodedQuery).append(")\n");
            sb.append("- [🔍 百度搜索](https://www.baidu.com/s?wd=").append(encodedQuery).append(")\n");
            sb.append("- [📖 CSDN搜索](https://so.csdn.net/so/search?q=").append(keywordQuery).append(")\n");

            return sb.toString().trim();
        }

        List<String> relevantChunks = retrieveCached(question, kbId);
        if (!relevantChunks.isEmpty()) {
            Set<String> sources = new java.util.LinkedHashSet<>();
            for (String chunk : relevantChunks) {
                if (chunk.contains("[来源:")) {
                    int start = chunk.indexOf("[来源:");
                    int end = chunk.indexOf("]", start);
                    if (end > start) {
                        sources.add(chunk.substring(start + 4, end));
                    }
                }
            }
            if (!sources.isEmpty()) {
                StringBuilder sb = new StringBuilder("📚 参考资料：\n");
                for (String source : sources) {
                    sb.append("- ").append(source).append("\n");
                }
                return sb.toString().trim();
            }
            return "📚 参考资料：《" + course + "》课程资料";
        }
        return null;
    }

    /** 优先复用 answer 阶段缓存的教材检索结果，避免同一问题检索两次 */
    private List<String> retrieveCached(String question, Long kbId) {
        RetrievalCache cache = cachedRetrieval.get();
        if (cache != null && cache.matches(question, kbId)) {
            return cache.chunks;
        }
        return embeddingService.retrieve(question, 8, kbId);
    }
}
