package com.xidian.osqa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xidian.osqa.entity.ChatMessage;
import com.xidian.osqa.entity.ChatSession;
import com.xidian.osqa.mapper.ChatMessageMapper;
import com.xidian.osqa.mapper.ChatSessionMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatService {

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final RagService ragService;

    public ChatService(ChatSessionMapper sessionMapper, ChatMessageMapper messageMapper, RagService ragService) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.ragService = ragService;
    }

    public List<ChatSession> getUserSessions(Long userId) {
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getUserId, userId)
               .orderByDesc(ChatSession::getUpdateTime);
        return sessionMapper.selectList(wrapper);
    }

    public ChatSession createSession(Long userId, String title) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(title != null ? title : "新对话");
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.insert(session);
        return session;
    }

    public List<ChatMessage> getSessionMessages(Long sessionId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)
               .orderByAsc(ChatMessage::getCreateTime);
        return messageMapper.selectList(wrapper);
    }

    public ChatMessage saveMessage(Long sessionId, String role, String content, String citation, String sourceType) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setCitation(citation);
        message.setSourceType(sourceType);
        message.setCreateTime(LocalDateTime.now());
        messageMapper.insert(message);
        return message;
    }

    public void updateSessionTitle(Long sessionId, String title) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session != null) {
            session.setTitle(title);
            session.setUpdateTime(LocalDateTime.now());
            sessionMapper.updateById(session);
        }
    }

    public void deleteSession(Long sessionId) {
        sessionMapper.deleteById(sessionId);
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId);
        messageMapper.delete(wrapper);
    }

    public String askQuestion(Long sessionId, String question, boolean webSearch) {
        return ragService.answer(question, webSearch);
    }

    public String getCitation(Long sessionId, String question, boolean webSearch) {
        return ragService.getCitation(question, webSearch);
    }
}
