package com.xidian.osqa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xidian.osqa.entity.ChatMessage;
import com.xidian.osqa.entity.ChatSession;
import com.xidian.osqa.entity.Clazz;
import com.xidian.osqa.mapper.ChatMessageMapper;
import com.xidian.osqa.mapper.ChatSessionMapper;
import com.xidian.osqa.mapper.ClazzMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final RagService ragService;
    private final ClazzMapper clazzMapper;
    private final KeywordAiService keywordAiService;

    public ChatService(ChatSessionMapper sessionMapper, ChatMessageMapper messageMapper, RagService ragService, ClazzMapper clazzMapper, KeywordAiService keywordAiService) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.ragService = ragService;
        this.clazzMapper = clazzMapper;
        this.keywordAiService = keywordAiService;
    }

    public List<ChatSession> getUserSessions(Long userId) {
        return getUserSessions(userId, null);
    }

    public List<ChatSession> getUserSessions(Long userId, Long classId) {
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getUserId, userId);
        if (classId != null) {
            wrapper.eq(ChatSession::getClassId, classId);
        }
        wrapper.orderByDesc(ChatSession::getUpdateTime);
        return sessionMapper.selectList(wrapper);
    }

    public ChatSession createSession(Long userId, String title, Long classId) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setClassId(classId);
        session.setTitle(title != null ? title : "新对话");
        session.setCreateTime(LocalDateTime.now());
        session.setUpdateTime(LocalDateTime.now());
        sessionMapper.insert(session);
        return session;
    }

    /** 解析会话所属班级对应的知识库ID，用于RAG检索作用域 */
    private Long resolveKbId(Long sessionId) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null || session.getClassId() == null) return null;
        Clazz clazz = clazzMapper.selectById(session.getClassId());
        return clazz != null ? clazz.getKbId() : null;
    }

    public List<ChatMessage> getSessionMessages(Long sessionId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)
               .orderByAsc(ChatMessage::getCreateTime);
        return messageMapper.selectList(wrapper);
    }

    public ChatMessage saveMessage(Long sessionId, String role, String content, String citation, String sourceType) {
        return saveMessage(sessionId, role, content, citation, sourceType, true);
    }

    /**
     * 保存消息。
     * @param extractKeywords 是否在 user 消息入库后异步提取知识点关键词。
     *                        当先保存问题、后再根据 AI 回答判定是否相关时，可传 false 延迟提取。
     */
    public ChatMessage saveMessage(Long sessionId, String role, String content, String citation, String sourceType, boolean extractKeywords) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setCitation(citation);
        message.setSourceType(sourceType);
        message.setCreateTime(LocalDateTime.now());
        messageMapper.insert(message);
        // 学生提问异步提取知识点关键词回填 keywords 字段，供热词统计聚合
        if ("user".equals(role) && extractKeywords) {
            keywordAiService.extractAndSaveAsync(message.getId(), content);
        }
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

    /** 延迟触发关键词提取（供 Controller 在确认非无关问题后调用） */
    public void extractKeywordsAsync(Long messageId, String content) {
        keywordAiService.extractAndSaveAsync(messageId, content);
    }

    /** 会话是否绑定到某个班级（用于统计归属判断） */
    public boolean sessionHasClass(Long sessionId) {
        ChatSession session = sessionMapper.selectById(sessionId);
        return session != null && session.getClassId() != null;
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
        return ragService.answer(question, webSearch, resolveKbId(sessionId));
    }

    public String getCitation(Long sessionId, String question, boolean webSearch) {
        return ragService.getCitation(question, webSearch, resolveKbId(sessionId));
    }

    public int getUserQuestionCount(Long userId) {
        return getUserQuestionCount(userId, null);
    }

    public int getUserQuestionCount(Long userId, Long classId) {
        return classId != null
                ? messageMapper.countUserQuestionsByClass(userId, classId)
                : messageMapper.countUserQuestions(userId);
    }

    public int getUserCitationRate(Long userId) {
        return getUserCitationRate(userId, null);
    }

    public int getUserCitationRate(Long userId, Long classId) {
        int total = classId != null
                ? messageMapper.countUserTotalAnswersByClass(userId, classId)
                : messageMapper.countUserTotalAnswers(userId);
        int cited = classId != null
                ? messageMapper.countUserCitedAnswersByClass(userId, classId)
                : messageMapper.countUserCitedAnswers(userId);
        return total > 0 ? Math.round(cited * 100f / total) : 0;
    }

    // 默认快捷提示（课程无关的通用学习提问模板）
    private static final List<String> DEFAULT_PROMPTS = Arrays.asList(
            "请解释这个知识点的核心概念", "举例说明它的应用场景", "常见误区有哪些",
            "与相关概念的区别是什么", "总结这一章的要点", "这个知识点通常如何考查"
    );

    public List<String> getQuickPrompts(Long userId) {
        return getQuickPrompts(userId, null);
    }

    public List<String> getQuickPrompts(Long userId, Long classId) {
        try {
            // 获取用户高频关键词（最多2个）
            List<Map<String, Object>> keywords = getUserKeywords(userId, classId, 2);

            List<String> prompts = new ArrayList<>();
            Set<String> usedKeywords = new HashSet<>();
            for (Map<String, Object> kw : keywords) {
                String word = (String) kw.get("word");
                prompts.add(word + "相关知识点");
                usedKeywords.add(word);
            }

            // 用通用模板补充到6个
            List<String> remaining = new ArrayList<>(DEFAULT_PROMPTS);
            Collections.shuffle(remaining, ThreadLocalRandom.current());
            for (String topic : remaining) {
                if (prompts.size() >= 6) break;
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

            log.info("生成快捷提示: userId={}, classId={}, prompts={}", userId, classId, prompts);
            return prompts;
        } catch (Exception e) {
            log.warn("生成快捷提示失败: {}", e.getMessage());
            return DEFAULT_PROMPTS;
        }
    }

    public List<Map<String, Object>> getUserKeywords(Long userId, int limit) {
        return getUserKeywords(userId, null, limit);
    }

    public List<Map<String, Object>> getUserKeywords(Long userId, Long classId, int limit) {
        try {
            List<String> kwJson = classId != null
                    ? messageMapper.findUserQuestionKeywordsByClass(userId, classId, limit * 3)
                    : messageMapper.findUserQuestionKeywords(userId, limit * 3);
            return keywordAiService.aggregate(kwJson, limit);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // ===== 学生个人新增统计维度 =====

    public Map<String, Object> getUserTrend(Long userId, String startDate, String endDate, String granularity) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (startDate == null || endDate == null) {
                LocalDate end = LocalDate.now();
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                endDate = end.format(fmt) + " 23:59:59";
                // 每日和每周都统计全部历史数据
                startDate = "2000-01-01 00:00:00";
            }
            List<Map<String, Object>> trend;
            if ("weekly".equals(granularity)) {
                trend = messageMapper.findUserWeeklyQuestionTrend(userId, startDate, endDate);
            } else {
                trend = messageMapper.findUserDailyQuestionTrend(userId, startDate, endDate);
            }
            result.put("trend", trend);
        } catch (Exception e) {
            result.put("trend", new ArrayList<>());
        }
        return result;
    }

    public Map<String, Object> getUserSessionRounds(Long userId, String startDate, String endDate, int limit) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (startDate == null || endDate == null) {
                LocalDate end = LocalDate.now();
                LocalDate start = end.minusDays(29);
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                startDate = start.format(fmt) + " 00:00:00";
                endDate = end.format(fmt) + " 23:59:59";
            }
            List<Map<String, Object>> rounds = messageMapper.findUserSessionRounds(userId, startDate, endDate, limit);
            result.put("rounds", rounds);
            int totalRounds = 0;
            for (Map<String, Object> r : rounds) {
                Object val = r.get("ROUNDS");
                if (val == null) val = r.get("rounds");
                if (val != null) totalRounds += ((Number) val).intValue();
            }
            result.put("avgRounds", rounds.isEmpty() ? 0 : Math.round(totalRounds * 100.0 / rounds.size()) / 100.0);
            result.put("totalSessions", rounds.size());
        } catch (Exception e) {
            result.put("rounds", new ArrayList<>());
            result.put("avgRounds", 0);
            result.put("totalSessions", 0);
        }
        return result;
    }

    public Map<String, Object> getUserSourceDistribution(Long userId, String startDate, String endDate) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (startDate == null || endDate == null) {
                LocalDate end = LocalDate.now();
                LocalDate start = end.minusDays(29);
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                startDate = start.format(fmt) + " 00:00:00";
                endDate = end.format(fmt) + " 23:59:59";
            }
            List<Map<String, Object>> dist = messageMapper.findUserSourceTypeDistribution(userId, startDate, endDate);
            result.put("distribution", dist);
        } catch (Exception e) {
            result.put("distribution", new ArrayList<>());
        }
        return result;
    }

    public Map<String, Object> getUserActiveDays(Long userId, String startDate, String endDate) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (startDate == null || endDate == null) {
                LocalDate end = LocalDate.now();
                LocalDate start = end.minusDays(29);
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                startDate = start.format(fmt) + " 00:00:00";
                endDate = end.format(fmt) + " 23:59:59";
            }
            int activeDays = messageMapper.findUserActiveDays(userId, startDate, endDate);
            List<String> dates = messageMapper.findUserActiveDates(userId, startDate, endDate);
            // 计算最大连续天数
            int maxStreak = 0;
            int currentStreak = 0;
            LocalDate prev = null;
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            for (String d : dates) {
                LocalDate date = LocalDate.parse(d, fmt);
                if (prev != null && prev.minusDays(1).equals(date)) {
                    currentStreak++;
                } else {
                    currentStreak = 1;
                }
                maxStreak = Math.max(maxStreak, currentStreak);
                prev = date;
            }
            result.put("activeDays", activeDays);
            result.put("maxStreak", maxStreak);
            result.put("dates", dates);
        } catch (Exception e) {
            result.put("activeDays", 0);
            result.put("maxStreak", 0);
            result.put("dates", new ArrayList<>());
        }
        return result;
    }
}
