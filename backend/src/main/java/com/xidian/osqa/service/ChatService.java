package com.xidian.osqa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xidian.osqa.entity.ChatMessage;
import com.xidian.osqa.entity.ChatSession;
import com.xidian.osqa.mapper.ChatMessageMapper;
import com.xidian.osqa.mapper.ChatSessionMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

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

    public List<Map<String, Object>> getUserKeywords(Long userId, int limit) {
        List<Map<String, Object>> keywords = new ArrayList<>();
        try {
            List<String> questions = messageMapper.findUserQuestionContents(userId, limit * 3);
            Map<String, Integer> wordCount = new HashMap<>();
            String[] osKeywords = {"进程", "线程", "死锁", "信号量", "内存", "页面置换",
                    "LRU", "调度", "文件系统", "中断", "同步", "互斥", "虚拟内存",
                    "分页", "分段", "缓冲", "管道", "套接字", "PV操作", "银行家算法",
                    "进程通信", "进程调度", "作业调度", "磁盘调度", "内存分配", "内存保护",
                    "抖动", "缺页", "快表", "位图", "空闲链表", "伙伴系统",
                    "索引节点", "目录", "超级块", "FAT", "RAID", "SPOOLing",
                    "用户态", "内核态", "系统调用", "上下文切换", "时间片", "优先级",
                    "临界区", "管程", "条件变量", "读写锁", "自旋锁",
                    "生产者消费者", "读者写者", "哲学家就餐", "饥饿", "活锁",
                    "安全序列", "资源分配图", "抢占", "非抢占", "死锁避免", "死锁检测",
                    "死锁预防", "死锁恢复", "页表", "多级页表", "反置页表",
                    "TLB", "局部性原理", "工作集", "置换算法", "FIFO", "OPT", "CLOCK",
                    "覆盖", "交换", "虚拟页式", "实页式", "段页式",
                    "文件控制块", "文件目录", "空闲空间管理", "连续分配", "链接分配",
                    "索引分配", "磁盘结构", "寻道", "旋转延迟", "传输时间",
                    "I/O软件", "设备驱动", "中断处理", "DMA", "通道",
                    "进程控制块", "PCB", "进程状态", "就绪", "阻塞", "运行"};

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
