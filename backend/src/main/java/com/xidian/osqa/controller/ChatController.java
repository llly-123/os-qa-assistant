package com.xidian.osqa.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xidian.osqa.common.Result;
import com.xidian.osqa.entity.ChatMessage;
import com.xidian.osqa.entity.ChatSession;
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
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
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
    public SseEmitter streamChat(@PathVariable Long sessionId, @RequestBody Map<String, Object> body) {
        String content = (String) body.get("content");
        Boolean webSearch = (Boolean) body.getOrDefault("webSearch", false);

        chatService.saveMessage(sessionId, "user", content, null, null);

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

                String answer = chatService.askQuestion(sessionId, content, webSearch != null && webSearch);
                String citation = chatService.getCitation(sessionId, content, webSearch != null && webSearch);

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
    public Result<?> askNonStream(@PathVariable Long sessionId, @RequestBody Map<String, Object> body) {
        String content = (String) body.get("content");
        Boolean webSearch = (Boolean) body.getOrDefault("webSearch", false);

        chatService.saveMessage(sessionId, "user", content, null, null);
        chatService.autoTitleIfNeeded(sessionId, content);

        String answer = chatService.askQuestion(sessionId, content, webSearch != null && webSearch);
        String citation = chatService.getCitation(sessionId, content, webSearch != null && webSearch);

        chatService.saveMessage(sessionId, "assistant", answer, citation, null);

        Map<String, Object> result = new HashMap<>();
        result.put("content", answer != null ? answer : "");
        result.put("citation", citation != null ? citation : "");
        return Result.success(result);
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
}
