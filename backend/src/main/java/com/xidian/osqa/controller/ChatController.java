package com.xidian.osqa.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xidian.osqa.common.NetworkUtil;
import com.xidian.osqa.common.Result;
import com.xidian.osqa.entity.ChatMessage;
import com.xidian.osqa.entity.ChatSession;
import com.xidian.osqa.security.PromptInjectionFilter;
import com.xidian.osqa.security.RateLimiter;
import com.xidian.osqa.service.ChatService;
import com.xidian.osqa.service.StudyTimeService;
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
    private final StudyTimeService studyTimeService;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    public ChatController(ChatService chatService, PromptInjectionFilter promptInjectionFilter, RateLimiter rateLimiter, StudyTimeService studyTimeService) {
        this.chatService = chatService;
        this.promptInjectionFilter = promptInjectionFilter;
        this.rateLimiter = rateLimiter;
        this.studyTimeService = studyTimeService;
    }

    @GetMapping("/sessions")
    public Result<?> getSessions(HttpServletRequest request, @RequestParam(required = false) Long classId) {
        Long userId = (Long) request.getAttribute("userId");
        List<ChatSession> sessions = chatService.getUserSessions(userId, classId);
        return Result.success(sessions);
    }

    @PostMapping("/sessions")
    public Result<?> createSession(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        String title = body.get("title") == null ? "新对话" : body.get("title").toString();
        Long classId = body.get("classId") == null ? null : Long.valueOf(body.get("classId").toString());
        ChatSession session = chatService.createSession(userId, title, classId);
        return Result.success(session);
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public Result<?> getMessages(@PathVariable Long sessionId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (!chatService.isSessionOwner(sessionId, userId)) {
            return Result.error(403, "无权访问此对话");
        }
        List<ChatMessage> messages = chatService.getSessionMessages(sessionId);
        return Result.success(messages);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public Result<?> deleteSession(@PathVariable Long sessionId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (!chatService.isSessionOwner(sessionId, userId)) {
            return Result.error(403, "无权操作此对话");
        }
        chatService.deleteSession(sessionId);
        return Result.success();
    }

    @PutMapping("/sessions/{sessionId}")
    public Result<?> updateSessionTitle(@PathVariable Long sessionId, @RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (!chatService.isSessionOwner(sessionId, userId)) {
            return Result.error(403, "无权操作此对话");
        }
        String title = body.get("title");
        chatService.updateSessionTitle(sessionId, title);
        return Result.success();
    }

    @PostMapping(value = "/sessions/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@PathVariable Long sessionId, @RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        // 会话归属校验
        if (!chatService.isSessionOwner(sessionId, userId)) {
            SseEmitter emitter = new SseEmitter();
            try {
                String errorJson = objectMapper.writeValueAsString(Map.of("type", "error", "message", "无权操作此对话"));
                emitter.send(SseEmitter.event().name("message").data(errorJson));
                emitter.complete();
            } catch (Exception ignored) {}
            return emitter;
        }
        String clientIp = NetworkUtil.getClientIp(request);

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

        // 检查会话是否绑定到班级
        final boolean inClass = chatService.sessionHasClass(sessionId);

        // 如果有视频上下文，附加到问题中
        final String effectiveContent;
        if (videoContext != null && !videoContext.isEmpty()) {
            effectiveContent = "[当前正在观看视频：" + videoContext + "]\n" + sanitizedContent;
        } else if (!inClass) {
            effectiveContent = "[注意：该学生未进入班级，教师无法统计其问答情况]\n" + sanitizedContent;
        } else {
            effectiveContent = sanitizedContent;
        }

        chatService.saveMessage(sessionId, "user", sanitizedContent, null, inClass ? "textbook" : "no_class");

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

                String streamSourceType = (webSearch != null && webSearch) ? "web" : "textbook";
                chatService.saveMessage(sessionId, "assistant", answer, citation, streamSourceType);

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
        Long userId = (Long) request.getAttribute("userId");

        // 会话归属校验
        if (!chatService.isSessionOwner(sessionId, userId)) {
            return Result.error(403, "无权操作此对话");
        }

        try {
            String clientIp = NetworkUtil.getClientIp(request);

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

        // 检查会话是否绑定到班级
        final boolean inClass = chatService.sessionHasClass(sessionId);

        // 如果有视频上下文，附加到问题中
        final String effectiveContent;
        if (videoContext != null && !videoContext.isEmpty()) {
            effectiveContent = "[当前正在观看视频：" + videoContext + "]\n" + sanitizedContent;
        } else if (!inClass) {
            effectiveContent = "[注意：该学生未进入班级，教师无法统计其问答情况]\n" + sanitizedContent;
        } else {
            effectiveContent = sanitizedContent;
        }

        // 先保存 user 消息，但暂不提取关键词（等 AI 回答后判定是否为无关问题再决定）
        com.xidian.osqa.entity.ChatMessage userMsg = chatService.saveMessage(sessionId, "user", sanitizedContent, null, inClass ? "textbook" : "no_class", false);
        chatService.autoTitleIfNeeded(sessionId, sanitizedContent);

            String answer = chatService.askQuestion(sessionId, effectiveContent, webSearch != null && webSearch);
            String citation = chatService.getCitation(sessionId, effectiveContent, webSearch != null && webSearch);

            String assistantSourceType = (webSearch != null && webSearch) ? "web" : "textbook";
            chatService.saveMessage(sessionId, "assistant", answer, citation, assistantSourceType);

            // AI 回答后再决定是否提取关键词：若被判为无关问题（拒答），不统计热词
            if (!isIrrelevantAnswer(answer)) {
                chatService.extractKeywordsAsync(userMsg.getId(), sanitizedContent);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("content", answer != null ? answer : "");
            result.put("citation", citation != null ? citation : "");
            return Result.success(result);
        } catch (Exception e) {
            log.error("askNonStream处理失败: sessionId={}", sessionId, e);
            return Result.error(500, "处理失败，请稍后重试");
        }
    }

    /** 判定 AI 回答是否为"无关问题拒答"，用于决定是否提取关键词统计热词 */
    private boolean isIrrelevantAnswer(String answer) {
        if (answer == null || answer.isBlank()) return true;
        String a = answer.trim();
        // 短回答且明确含"无关"字样，视为拒答
        if (a.length() <= 60 && a.contains("无关")) return true;
        // 兜底空内容
        return a.isEmpty();
    }

    @GetMapping("/my-stats")
    public Result<?> getMyStats(HttpServletRequest request, @RequestParam(required = false) Long classId) {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalQuestions", chatService.getUserQuestionCount(userId, classId));
        stats.put("citationRate", chatService.getUserCitationRate(userId, classId));
        stats.put("keywords", chatService.getUserKeywords(userId, classId, 20));
        return Result.success(stats);
    }

    @GetMapping("/my-trend")
    public Result<?> getMyTrend(
            HttpServletRequest request,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "daily") String granularity) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(chatService.getUserTrend(userId, classId, startDate, endDate, granularity));
    }

    @GetMapping("/my-sessions")
    public Result<?> getMySessionRounds(
            HttpServletRequest request,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "20") int limit) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(chatService.getUserSessionRounds(userId, classId, startDate, endDate, limit));
    }

    @GetMapping("/my-sources")
    public Result<?> getMySourceDistribution(
            HttpServletRequest request,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(chatService.getUserSourceDistribution(userId, classId, startDate, endDate));
    }

    @GetMapping("/my-active-days")
    public Result<?> getMyActiveDays(
            HttpServletRequest request,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(chatService.getUserActiveDays(userId, classId, startDate, endDate));
    }

    @GetMapping("/my-hours")
    public Result<?> getMyHourlyDistribution(
            HttpServletRequest request,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(chatService.getUserHourlyDistribution(userId, classId, startDate, endDate));
    }

    // ===== 学习时长统计 =====

    /** 学生上报学习时长（登录课程界面期间周期性心跳上报） */
    @PostMapping("/study-time")
    public Result<?> reportStudyTime(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        Long classId = body.get("classId") == null ? null : Long.valueOf(body.get("classId").toString());
        int seconds = body.get("seconds") == null ? 0 : Integer.parseInt(body.get("seconds").toString());
        if (classId == null || seconds <= 0) {
            return Result.error(400, "参数无效");
        }
        studyTimeService.report(userId, classId, seconds);
        return Result.success();
    }

    /** 我的学习时长（可按班级过滤） */
    @GetMapping("/my-study-time")
    public Result<?> getMyStudyTime(
            HttpServletRequest request,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(studyTimeService.getUserStudySeconds(userId, classId, startDate, endDate));
    }

    @GetMapping("/quick-prompts")
    public Result<?> getQuickPrompts(HttpServletRequest request, @RequestParam(required = false) Long classId) {
        Long userId = (Long) request.getAttribute("userId");
        List<String> prompts = chatService.getQuickPrompts(userId, classId);
        return Result.success(prompts);
    }

}
