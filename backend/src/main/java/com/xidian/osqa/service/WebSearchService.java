package com.xidian.osqa.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class WebSearchService {

    private static final Logger log = LoggerFactory.getLogger(WebSearchService.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ChatLanguageModel chatModel;

    @Value("${web.search.enabled:true}")
    private boolean searchEnabled;

    public WebSearchService(ChatLanguageModel chatModel) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        this.chatModel = chatModel;
    }

    /**
     * 执行联网搜索
     * @param question 学生问题
     * @param subject 学科名称（如：操作系统）
     * @return 搜索结果列表
     */
    public List<SearchResult> search(String question, String subject) {
        if (!searchEnabled) {
            log.info("联网搜索功能已禁用");
            return new ArrayList<>();
        }

        try {
            String searchQuery = buildSearchQuery(question, subject);
            log.info("执行联网搜索: {}", searchQuery);

            // 使用DuckDuckGo Instant Answer API
            String encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
            String url = "https://api.duckduckgo.com/?q=" + encodedQuery + "&format=json&no_html=1";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseDuckDuckGoResponse(response.body(), searchQuery);
            } else {
                log.warn("搜索请求失败: HTTP {}", response.statusCode());
                return new ArrayList<>();
            }
        } catch (Exception e) {
            log.error("联网搜索异常", e);
            return new ArrayList<>();
        }
    }

    /**
     * 构建搜索查询，限制在特定学科范围内
     */
    private String buildSearchQuery(String question, String subject) {
        return subject + " " + question + " 知识点 原理";
    }

    /**
     * 解析DuckDuckGo响应
     */
    private List<SearchResult> parseDuckDuckGoResponse(String jsonBody, String query) {
        List<SearchResult> results = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(jsonBody);

            // 解析Abstract（主要答案）
            JsonNode abstractNode = root.get("Abstract");
            if (abstractNode != null && !abstractNode.asText().isEmpty()) {
                SearchResult result = new SearchResult();
                result.setTitle(root.get("Heading") != null ? root.get("Heading").asText() : query);
                result.setContent(abstractNode.asText());
                result.setUrl(root.get("AbstractURL") != null ? root.get("AbstractURL").asText() : "");
                result.setSource("DuckDuckGo");
                results.add(result);
            }

            // 解析RelatedTopics（相关主题）
            JsonNode relatedTopics = root.get("RelatedTopics");
            if (relatedTopics != null && relatedTopics.isArray()) {
                for (JsonNode topic : relatedTopics) {
                    if (results.size() >= 5) break; // 限制结果数量

                    JsonNode textNode = topic.get("Text");
                    if (textNode != null && !textNode.asText().isEmpty()) {
                        SearchResult result = new SearchResult();
                        result.setTitle(topic.get("FirstURL") != null ? topic.get("FirstURL").asText() : query);
                        result.setContent(textNode.asText());
                        result.setUrl(topic.get("FirstURL") != null ? topic.get("FirstURL").asText() : "");
                        result.setSource("DuckDuckGo");
                        results.add(result);
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析搜索响应失败", e);
        }

        return results;
    }

    /**
     * 使用DeepSeek整理搜索结果
     */
    public String summarizeWithAI(List<SearchResult> results, String originalQuestion) {
        if (results.isEmpty()) {
            return null;
        }

        try {
            StringBuilder context = new StringBuilder();
            context.append("以下是网络搜索到的关于「").append(originalQuestion).append("」的相关内容：\n\n");

            for (int i = 0; i < results.size(); i++) {
                SearchResult r = results.get(i);
                context.append("【搜索结果").append(i + 1).append("】\n")
                       .append("标题: ").append(r.getTitle()).append("\n")
                       .append("内容: ").append(r.getContent()).append("\n")
                       .append("来源: ").append(r.getSource()).append("\n\n");
            }

            context.append("\n请基于以上网络搜索结果，为学生提供清晰、准确的回答。\n");
            context.append("要求：\n");
            context.append("1. 整合多个搜索结果，给出完整答案\n");
            context.append("2. 使用Markdown格式\n");
            context.append("3. 在回答末尾明确标注：**【来源：网络搜索】**\n");
            context.append("4. 如果搜索结果不足以回答问题，请说明\n\n");
            context.append("学生问题：").append(originalQuestion);

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new SystemMessage("你是一个知识助手，负责整理和总结网络搜索结果，帮助学生理解问题。"));
            messages.add(new UserMessage(context.toString()));

            Response<AiMessage> response = chatModel.generate(messages);
            return response.content().text();
        } catch (Exception e) {
            log.error("AI整理搜索结果失败", e);

            // 降级：直接返回原始搜索结果
            StringBuilder fallback = new StringBuilder();
            fallback.append("根据网络搜索，找到以下相关信息：\n\n");
            for (SearchResult r : results) {
                fallback.append("- **").append(r.getTitle()).append("**: ")
                       .append(r.getContent()).append("\n\n");
            }
            fallback.append("\n**【来源：网络搜索】**");
            return fallback.toString();
        }
    }

    /**
     * 搜索结果实体类
     */
    public static class SearchResult {
        private String title;
        private String content;
        private String url;
        private String source;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
    }
}
