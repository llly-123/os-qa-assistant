package com.xidian.osqa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xidian.osqa.config.AiModelProvider;
import com.xidian.osqa.entity.ChatMessage;
import com.xidian.osqa.mapper.ChatMessageMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 用 AI（DeepSeek）从学生提问中提取"知识点关键词"，替代原先 jieba 分词+停用词的规则方案。
 *
 * - 提问入库后异步提取并回填 chat_message.keywords（JSON 数组），不阻塞提问回答流程
 * - 统计页直接聚合 keywords 字段词频，无需实时分词
 * - 启动时异步补跑存量（keywords 为空的 user 消息），保证历史数据也覆盖
 * - AI 失败时 keywords 留空，该提问不贡献热词（降级）
 */
@Service
public class KeywordAiService {

    private static final Logger log = LoggerFactory.getLogger(KeywordAiService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AiModelProvider aiModelProvider;
    private final ChatMessageMapper messageMapper;

    /** 单线程串行执行，避免并发打爆 DeepSeek 限流 */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "keyword-ai");
        t.setDaemon(true);
        return t;
    });

    private static final String PROMPT_TEMPLATE = """
            你是关键词提取助手。从学生的提问中提取核心"知识点关键词"--即专业术语、概念名、技术名词。
            要求：
            1. 只提取知识点术语，排除动词(如 判断/解释/说明/分析/比较)、泛词(如 状态/算法/消息/工作/过程/方式/情况)、助词、错别字。
            2. 用规范术语，纠正提问中的错别字(如"对列"识别为"队列")；若词是某器件/概念的简称且语义明确，归一为规范名(如"晶体"若指器件归为"晶体管")。
            3. 每条提问提取 1-5 个关键词；若无明确知识点，返回空数组 []。
            4. 只返回 JSON 数组，例如 ["晶体管","负反馈"]，不要任何其他文字。
            提问：%s
            """;

    public KeywordAiService(AiModelProvider aiModelProvider, ChatMessageMapper messageMapper) {
        this.aiModelProvider = aiModelProvider;
        this.messageMapper = messageMapper;
    }

    /** 调 AI 提取知识点关键词 */
    public List<String> extractKeywords(String question) {
        if (question == null || question.isBlank()) return Collections.emptyList();
        try {
            List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
            messages.add(new UserMessage(String.format(PROMPT_TEMPLATE, question)));
            Response<AiMessage> resp = aiModelProvider.getModel().generate(messages);
            return parseKeywords(resp.content().text());
        } catch (Exception e) {
            log.warn("AI提取关键词失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /** 解析 AI 返回的 JSON 数组（容错：抽取首个 [ ... ] 片段） */
    private List<String> parseKeywords(String text) {
        if (text == null) return Collections.emptyList();
        String t = text.trim();
        int start = t.indexOf('[');
        int end = t.lastIndexOf(']');
        if (start < 0 || end <= start) return Collections.emptyList();
        try {
            List<?> list = MAPPER.readValue(t.substring(start, end + 1), List.class);
            List<String> result = new ArrayList<>();
            for (Object o : list) {
                if (o != null) {
                    String s = o.toString().trim();
                    if (!s.isEmpty()) result.add(s);
                }
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /** 异步提取并回填 keywords 字段（fire-and-forget，不阻塞提问） */
    public void extractAndSaveAsync(Long messageId, String content) {
        if (messageId == null) return;
        executor.submit(() -> {
            try {
                List<String> kws = extractKeywords(content);
                String json = kws.isEmpty() ? "[]" : MAPPER.writeValueAsString(kws);
                ChatMessage patch = new ChatMessage();
                patch.setId(messageId);
                patch.setKeywords(json);
                messageMapper.updateById(patch);
                log.info("关键词已回填: messageId={}, keywords={}", messageId, json);
            } catch (Exception e) {
                log.warn("关键词回填失败: messageId={}, err={}", messageId, e.getMessage());
            }
        });
    }

    /** 聚合 keywords 字段（JSON 数组列表）词频，取 top N */
    public List<Map<String, Object>> aggregate(List<String> keywordsJsonList, int limit) {
        Map<String, Integer> count = new HashMap<>();
        if (keywordsJsonList != null) {
            for (String json : keywordsJsonList) {
                Set<String> seen = new HashSet<>(); // 单条提问内去重
                for (String kw : parseKeywords(json)) {
                    if (seen.add(kw)) count.merge(kw, 1, Integer::sum);
                }
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        count.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(limit)
                .forEach(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("word", e.getKey());
                    m.put("count", e.getValue());
                    result.add(m);
                });
        return result;
    }

    /** 应用启动后异步补跑：为没有 keywords 的 user 消息提取回填 */
    @EventListener(ApplicationReadyEvent.class)
    public void backfillExisting() {
        executor.submit(() -> {
            try {
                LambdaQueryWrapper<ChatMessage> w = new LambdaQueryWrapper<>();
                w.eq(ChatMessage::getRole, "user")
                        .and(q -> q.isNull(ChatMessage::getKeywords).or().eq(ChatMessage::getKeywords, ""))
                        .last("LIMIT 500");
                List<ChatMessage> list = messageMapper.selectList(w);
                log.info("补跑存量关键词提取: {} 条", list.size());
                for (ChatMessage m : list) {
                    try {
                        List<String> kws = extractKeywords(m.getContent());
                        String json = kws.isEmpty() ? "[]" : MAPPER.writeValueAsString(kws);
                        m.setKeywords(json);
                        messageMapper.updateById(m);
                    } catch (Exception e) {
                        log.warn("补跑单条失败: id={}, err={}", m.getId(), e.getMessage());
                    }
                }
                log.info("补跑存量关键词完成");
            } catch (Exception e) {
                log.error("补跑存量关键词失败", e);
            }
        });
    }
}
