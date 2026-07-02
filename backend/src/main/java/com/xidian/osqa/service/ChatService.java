package com.xidian.osqa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xidian.osqa.common.OsConstants;
import com.xidian.osqa.entity.ChatMessage;
import com.xidian.osqa.entity.ChatSession;
import com.xidian.osqa.mapper.ChatMessageMapper;
import com.xidian.osqa.mapper.ChatSessionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

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

    public boolean isSessionOwner(Long sessionId, Long userId) {
        ChatSession session = sessionMapper.selectById(sessionId);
        return session != null && session.getUserId().equals(userId);
    }

    public void deleteSession(Long sessionId) {
        sessionMapper.deleteById(sessionId);
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId);
        messageMapper.delete(wrapper);
    }

    public void autoTitleIfNeeded(Long sessionId, String question) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session != null && "新对话".equals(session.getTitle())) {
            String title = question.length() > 20 ? question.substring(0, 20) + "..." : question;
            session.setTitle(title);
            session.setUpdateTime(LocalDateTime.now());
            sessionMapper.updateById(session);
        } else if (session != null) {
            session.setUpdateTime(LocalDateTime.now());
            sessionMapper.updateById(session);
        }
    }

    public String askQuestion(Long sessionId, String question, boolean webSearch) {
        return ragService.answer(question, webSearch);
    }

    public String getCitation(Long sessionId, String question, boolean webSearch) {
        return ragService.getCitation(question, webSearch);
    }

    public int getUserQuestionCount(Long userId) {
        return messageMapper.countUserQuestions(userId);
    }

    public int getUserCitationRate(Long userId) {
        int total = messageMapper.countUserTotalAnswers(userId);
        int cited = messageMapper.countUserCitedAnswers(userId);
        return total > 0 ? Math.round(cited * 100 / total) : 0;
    }

    // 默认快捷提示（新用户或无历史时使用）
    private static final List<String> DEFAULT_PROMPTS = Arrays.asList(
            "什么是进程死锁？", "死锁的四个必要条件", "LRU算法原理",
            "页面置换算法", "信号量与P/V操作", "进程调度算法"
    );

    // 操作系统核心知识点，用于生成动态提示
    private static final String[] OS_TOPICS = {
            "进程与线程的区别", "死锁的四个必要条件", "LRU算法原理",
            "页面置换算法有哪些", "信号量与P/V操作", "进程调度算法",
            "银行家算法", "生产者消费者问题", "虚拟内存管理",
            "文件系统结构", "磁盘调度算法", "进程同步与互斥",
            "内存分配方式", "分段与分页的区别", "中断处理过程",
            "I/O控制方式", "SPOOLing技术", "管道通信",
            "读者写者问题", "哲学家就餐问题", "作业调度算法",
            "死锁避免与预防", "抖动现象", "多级页表",
            "位图与空闲链表", "FAT文件系统", "RAID技术",
            "DMA与中断", "用户态与内核态", "系统调用过程"
    };

    public List<String> getQuickPrompts(Long userId) {
        try {
            // 获取用户高频关键词（最多2个）
            List<Map<String, Object>> keywords = getUserKeywords(userId, 2);

            List<String> prompts = new ArrayList<>();

            // 基于用户高频关键词生成个性化提示（最多2个）
            Set<String> usedKeywords = new HashSet<>();
            for (Map<String, Object> kw : keywords) {
                String word = (String) kw.get("word");
                prompts.add(word + "相关知识点");
                usedKeywords.add(word);
            }

            // 从题库中随机补充到6个
            List<String> remaining = new ArrayList<>(Arrays.asList(OS_TOPICS));
            Collections.shuffle(remaining, ThreadLocalRandom.current());
            for (String topic : remaining) {
                if (prompts.size() >= 6) break;
                // 避免与关键词提示的主题重叠
                boolean overlap = false;
                for (String kw : usedKeywords) {
                    if (topic.contains(kw)) {
                        overlap = true;
                        break;
                    }
                }
                if (!overlap) {
                    prompts.add(topic);
                }
            }

            log.info("生成快捷提示: userId={}, prompts={}", userId, prompts);
            return prompts;
        } catch (Exception e) {
            log.warn("生成快捷提示失败: {}", e.getMessage());
            return DEFAULT_PROMPTS;
        }
    }

    public List<Map<String, Object>> getUserKeywords(Long userId, int limit) {
        List<Map<String, Object>> keywords = new ArrayList<>();
        try {
            List<String> questions = messageMapper.findUserQuestionContents(userId, limit * 3);
            Map<String, Integer> wordCount = new HashMap<>();
            String[] osKeywords = OsConstants.OS_KEYWORDS;

            for (String question : questions) {
                if (question == null) continue;
                for (String keyword : osKeywords) {
                    if (question.contains(keyword)) {
                        wordCount.merge(keyword, 1, Integer::sum);
                    }
                }
            }

            wordCount.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(limit)
                    .forEach(entry -> {
                        Map<String, Object> kw = new HashMap<>();
                        kw.put("word", entry.getKey());
                        kw.put("count", entry.getValue());
                        keywords.add(kw);
                    });
        } catch (Exception e) {
            // return empty list
        }
        return keywords;
    }
}
