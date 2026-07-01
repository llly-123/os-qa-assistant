package com.xidian.osqa.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xidian.osqa.common.Result;
import com.xidian.osqa.entity.ChatMessage;
import com.xidian.osqa.entity.ChatSession;
import com.xidian.osqa.security.PromptInjectionFilter;
import com.xidian.osqa.security.RateLimiter;
import com.xidian.osqa.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ChatService chatService;
    private final PromptInjectionFilter promptInjectionFilter;
    private final RateLimiter rateLimiter;
    private final com.xidian.osqa.service.ClazzService clazzService;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public ChatController(ChatService chatService, PromptInjectionFilter promptInjectionFilter, RateLimiter rateLimiter, com.xidian.osqa.service.ClazzService clazzService) {
        this.chatService = chatService;
        this.promptInjectionFilter = promptInjectionFilter;
        this.rateLimiter = rateLimiter;
        this.clazzService = clazzService;
    }

    @GetMapping("/sessions")
    public Result<?> getSessions(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<ChatSession> sessions = chatService.getUserSessions(userId);
        return Result.success(sessions);
    }

    @PostMapping("/sessions")
    public Result<?> createSession(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        String title = body.getOrDefault("title", "新对话");
        ChatSession session = chatService.createSession(userId, title);
        return Result.success(session);
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public Result<?> getMessages(@PathVariable Long sessionId) {
        List<ChatMessage> messages = chatService.getSessionMessages(sessionId);
        return Result.success(messages);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public Result<?> deleteSession(@PathVariable Long sessionId) {
        chatService.deleteSession(sessionId);
        return Result.success();
    }

    @PutMapping("/sessions/{sessionId}")
    public Result<?> updateSessionTitle(@PathVariable Long sessionId, @RequestBody Map<String, String> body) {
        String title = body.get("title");
        chatService.updateSessionTitle(sessionId, title);
        return Result.success();
    }

    @PostMapping(value = "/sessions/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@PathVariable Long sessionId, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String clientIp = getClientIp(request);

        // 限流检查
        if (!rateLimiter.allowChatRequest(userId, clientIp)) {
            SseEmitter emitter = new SseEmitter();
            try {
                String errorJson = objectMapper.writeValueAsString(Map.of("type", "error", "message", "请求过于频繁，请稍后再试"));
                emitter.send(SseEmitter.event().name("message").data(errorJson));
                emitter.complete();
            } catch (Exception ignored) {}
            return emitter;
        }

        String content = (String) body.get("content");

        // Prompt注入检查
        String injectionWarning = promptInjectionFilter.checkInjection(content);
        if (injectionWarning != null) {
            SseEmitter emitter = new SseEmitter();
            try {
                String errorJson = objectMapper.writeValueAsString(Map.of("type", "error", "message", "输入内容存在安全风险，请修改后重试"));
                emitter.send(SseEmitter.event().name("message").data(errorJson));
                emitter.complete();
            } catch (Exception ignored) {}
            return emitter;
        }

        // 清洗输入
        final String sanitizedContent = promptInjectionFilter.sanitize(content);

        Boolean webSearch = (Boolean) body.getOrDefault("webSearch", false);
        String videoContext = (String) body.get("videoContext");

        // 检查是否在班级中
        var clazz = clazzService.getStudentActiveClass(userId);
        final boolean inClass = (clazz != null);

        // 如果有视频上下文，附加到问题中
        final String effectiveContent;
        if (videoContext != null && !videoContext.isEmpty()) {
            effectiveContent = "[当前正在观看视频：" + videoContext + "]\n" + sanitizedContent;
        } else if (!inClass) {
            effectiveContent = "[注意：该学生未进入班级，教师无法统计其问答情况]\n" + sanitizedContent;
        } else {
            effectiveContent = sanitizedContent;
        }

        chatService.saveMessage(sessionId, "user", sanitizedContent, null, inClass ? null : "no_class");

        SseEmitter emitter = new SseEmitter(180000L);

        emitter.onCompletion(() -> log.debug("SSE连接完成: sessionId={}", sessionId));
        emitter.onTimeout(() -> {
            log.warn("SSE连接超时: sessionId={}", sessionId);
            emitter.complete();
        });
        emitter.onError(e -> {
            log.warn("SSE连接错误: sessionId={}, error={}", sessionId, e.getMessage());
            emitter.complete();
        });

        executorService.execute(() -> {
            try {
                log.info("开始处理问题: sessionId={}, content={}", sessionId, content);

                String answer = chatService.askQuestion(sessionId, effectiveContent, webSearch != null && webSearch);
                String citation = chatService.getCitation(sessionId, effectiveContent, webSearch != null && webSearch);

                chatService.saveMessage(sessionId, "assistant", answer, citation, null);

                String contentJson = objectMapper.writeValueAsString(Map.of("type", "content", "content", answer));
                emitter.send(SseEmitter.event().name("message").data(contentJson));

                if (citation != null && !citation.isEmpty()) {
                    String citationJson = objectMapper.writeValueAsString(Map.of("type", "citation", "citation", citation));
                    emitter.send(SseEmitter.event().name("message").data(citationJson));
                }

                String doneJson = objectMapper.writeValueAsString(Map.of("type", "done"));
                emitter.send(SseEmitter.event().name("message").data(doneJson));

                emitter.complete();
                log.info("问题处理完成: sessionId={}", sessionId);
            } catch (Exception e) {
                log.error("处理问题失败: sessionId={}", sessionId, e);
                try {
                    String errorJson = objectMapper.writeValueAsString(Map.of("type", "error", "message", e.getMessage() != null ? e.getMessage() : "处理失败"));
                    emitter.send(SseEmitter.event().name("message").data(errorJson));
                    emitter.complete();
                } catch (Exception ignored) {
                    try { emitter.complete(); } catch (Exception ignored2) {}
                }
            }
        });

        return emitter;
    }

    @PostMapping("/sessions/{sessionId}/ask")
    public Result<?> askNonStream(@PathVariable Long sessionId, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            String clientIp = getClientIp(request);

            // 限流检查
            if (!rateLimiter.allowChatRequest(userId, clientIp)) {
                return Result.error(429, "请求过于频繁，请稍后再试");
            }

            String content = (String) body.get("content");

            // Prompt注入检查
            String injectionWarning = promptInjectionFilter.checkInjection(content);
            if (injectionWarning != null) {
                return Result.error(400, "输入内容存在安全风险，请修改后重试");
            }

            // 清洗输入
        final String sanitizedContent = promptInjectionFilter.sanitize(content);

        Boolean webSearch = (Boolean) body.getOrDefault("webSearch", false);
        String videoContext = (String) body.get("videoContext");

        // 检查是否在班级中
        var clazz = clazzService.getStudentActiveClass(userId);
        final boolean inClass = (clazz != null);

        // 如果有视频上下文，附加到问题中
        final String effectiveContent;
        if (videoContext != null && !videoContext.isEmpty()) {
            effectiveContent = "[当前正在观看视频：" + videoContext + "]\n" + sanitizedContent;
        } else if (!inClass) {
            effectiveContent = "[注意：该学生未进入班级，教师无法统计其问答情况]\n" + sanitizedContent;
        } else {
            effectiveContent = sanitizedContent;
        }

        chatService.saveMessage(sessionId, "user", sanitizedContent, null, inClass ? null : "no_class");
        chatService.autoTitleIfNeeded(sessionId, sanitizedContent);

            String answer = chatService.askQuestion(sessionId, effectiveContent, webSearch != null && webSearch);
            String citation = chatService.getCitation(sessionId, effectiveContent, webSearch != null && webSearch);

            chatService.saveMessage(sessionId, "assistant", answer, citation, null);

            Map<String, Object> result = new HashMap<>();
            result.put("content", answer != null ? answer : "");
            result.put("citation", citation != null ? citation : "");
            return Result.success(result);
        } catch (Exception e) {
            log.error("askNonStream处理失败: sessionId={}", sessionId, e);
            return Result.error(500, "处理失败: " + e.getMessage());
        }
    }

    @GetMapping("/my-stats")
    public Result<?> getMyStats(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalQuestions", chatService.getUserQuestionCount(userId));
        stats.put("citationRate", chatService.getUserCitationRate(userId));
        stats.put("keywords", chatService.getUserKeywords(userId, 20));
        return Result.success(stats);
    }

    @GetMapping("/quick-prompts")
    public Result<?> getQuickPrompts(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<String> prompts = chatService.getQuickPrompts(userId);
        return Result.success(prompts);
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
