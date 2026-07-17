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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

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
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.objectMapper = new ObjectMapper();
        this.chatModel = chatModel;
    }

    // URL有效性检查专用客户端（模拟浏览器）
    private static final String BROWSER_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36";
    private static final ExecutorService urlCheckExecutor = Executors.newFixedThreadPool(3);
    private static final Semaphore urlCheckSemaphore = new Semaphore(2); // 限制并发数

    /**
     * 并发检查搜索结果中URL的有效性
     * @param results 搜索结果列表
     */
    public void checkUrlValidity(List<SearchResult> results) {
        if (results == null || results.isEmpty()) return;

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (SearchResult result : results) {
            if (result.getUrl() == null || result.getUrl().isEmpty()) {
                result.setValid(false);
                continue;
            }
            // 跳过搜索引擎链接（始终有效）
            if (result.getUrl().contains("bing.com/search") || result.getUrl().contains("baidu.com/s?")) {
                result.setValid(true);
                continue;
            }

            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    urlCheckSemaphore.acquire();
                    boolean valid = checkSingleUrl(result.getUrl());
                    result.setValid(valid);
                    log.info("URL检查: {} -> {}", result.getUrl(), valid ? "有效" : "失效");
                } catch (InterruptedException e) {
                    result.setValid(null);
                } finally {
                    urlCheckSemaphore.release();
                }
            }, urlCheckExecutor));
        }

        // 等待所有检查完成，最多等5秒
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("URL检查超时，部分结果未完成验证");
        }
    }

    /**
     * 检查单个URL是否可访问
     */
    private boolean checkSingleUrl(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("User-Agent", BROWSER_USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int status = response.statusCode();
            // 2xx和3xx视为有效
            return status >= 200 && status < 400;
        } catch (Exception e) {
            // HEAD请求可能被拒绝，尝试GET请求（只读取头部）
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(5))
                        .header("User-Agent", BROWSER_USER_AGENT)
                        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                        .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(
                        java.nio.charset.StandardCharsets.UTF_8
                ));
                int status = response.statusCode();
                return status >= 200 && status < 400;
            } catch (Exception e2) {
                log.debug("URL检查失败: {} - {}", url, e2.getMessage());
                return false;
            }
        }
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
                    .timeout(Duration.ofSeconds(8))
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
            log.info("DuckDuckGo原始响应: {}", jsonBody.substring(0, Math.min(500, jsonBody.length())));

            // 解析Abstract（主要答案）
            JsonNode abstractNode = root.get("Abstract");
            if (abstractNode != null && !abstractNode.asText().isEmpty()) {
                SearchResult result = new SearchResult();
                result.setTitle(root.get("Heading") != null ? root.get("Heading").asText() : query);
                result.setContent(abstractNode.asText());
                String abstractUrl = root.get("AbstractURL") != null ? root.get("AbstractURL").asText() : "";
                result.setUrl(abstractUrl);
                result.setSource("DuckDuckGo");
                results.add(result);
                log.info("解析到Abstract结果: title={}, url={}", result.getTitle(), abstractUrl);
            }

            // 解析RelatedTopics（相关主题）
            JsonNode relatedTopics = root.get("RelatedTopics");
            if (relatedTopics != null && relatedTopics.isArray()) {
                for (JsonNode topic : relatedTopics) {
                    if (results.size() >= 5) break;

                    JsonNode textNode = topic.get("Text");
                    if (textNode != null && !textNode.asText().isEmpty()) {
                        SearchResult result = new SearchResult();
                        String firstUrl = topic.get("FirstURL") != null ? topic.get("FirstURL").asText() : "";
                        result.setTitle(firstUrl); // 用URL作为标题显示
                        result.setContent(textNode.asText());
                        result.setUrl(firstUrl);
                        result.setSource("DuckDuckGo");
                        results.add(result);
                        log.info("解析到RelatedTopic: url={}", firstUrl);
                    }
                }
            }
            
            log.info("共解析到{}个搜索结果", results.size());
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
        private Boolean valid; // null=未检查, true=有效, false=失效

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public Boolean getValid() { return valid; }
        public void setValid(Boolean valid) { this.valid = valid; }
    }
}
