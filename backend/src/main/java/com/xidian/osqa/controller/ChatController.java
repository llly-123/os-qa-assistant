package com.xidian.osqa.controller;

import com.xidian.osqa.common.Result;
import com.xidian.osqa.entity.ChatMessage;
import com.xidian.osqa.entity.ChatSession;
import com.xidian.osqa.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
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

    @PostMapping("/sessions/{sessionId}/stream")
    public SseEmitter streamChat(@PathVariable Long sessionId, @RequestBody Map<String, Object> body) {
        String content = (String) body.get("content");
        Boolean webSearch = (Boolean) body.getOrDefault("webSearch", false);

        chatService.saveMessage(sessionId, "user", content, null, null);

        SseEmitter emitter = new SseEmitter(120000L);

        executorService.execute(() -> {
            try {
                String answer = chatService.askQuestion(sessionId, content, webSearch != null && webSearch);
                String citation = chatService.getCitation(sessionId, content, webSearch != null && webSearch);

                ChatMessage aiMessage = chatService.saveMessage(sessionId, "assistant", answer, citation, null);

                Map<String, Object> contentEvent = new HashMap<>();
                contentEvent.put("type", "content");
                contentEvent.put("content", answer);
                emitter.send(SseEmitter.event().data(contentEvent));

                if (citation != null) {
                    Map<String, Object> citationEvent = new HashMap<>();
                    citationEvent.put("type", "citation");
                    citationEvent.put("citation", citation);
                    emitter.send(SseEmitter.event().data(citationEvent));
                }

                Map<String, Object> doneEvent = new HashMap<>();
                doneEvent.put("type", "done");
                emitter.send(SseEmitter.event().data(doneEvent));

                emitter.complete();
            } catch (Exception e) {
                try {
                    Map<String, Object> errorEvent = new HashMap<>();
                    errorEvent.put("type", "error");
                    errorEvent.put("message", e.getMessage());
                    emitter.send(SseEmitter.event().data(errorEvent));
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    @PostMapping("/sessions/{sessionId}/ask")
    public Result<?> askNonStream(@PathVariable Long sessionId, @RequestBody Map<String, Object> body) {
        String content = (String) body.get("content");
        Boolean webSearch = (Boolean) body.getOrDefault("webSearch", false);

        chatService.saveMessage(sessionId, "user", content, null, null);

        String answer = chatService.askQuestion(sessionId, content, webSearch != null && webSearch);
        String citation = chatService.getCitation(sessionId, content, webSearch != null && webSearch);

        chatService.saveMessage(sessionId, "assistant", answer, citation, null);

        Map<String, Object> result = new HashMap<>();
        result.put("content", answer);
        result.put("citation", citation);
        return Result.success(result);
    }
}
