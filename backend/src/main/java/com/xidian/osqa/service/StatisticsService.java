package com.xidian.osqa.service;

import com.xidian.osqa.mapper.ChatMessageMapper;
import com.xidian.osqa.mapper.ChatSessionMapper;
import com.xidian.osqa.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class StatisticsService {

    private final ChatMessageMapper messageMapper;
    private final ChatSessionMapper sessionMapper;
    private final UserMapper userMapper;

    public StatisticsService(ChatMessageMapper messageMapper, ChatSessionMapper sessionMapper, UserMapper userMapper) {
        this.messageMapper = messageMapper;
        this.sessionMapper = sessionMapper;
        this.userMapper = userMapper;
    }

    public Map<String, Object> getOverview() {
        Map<String, Object> overview = new HashMap<>();
        try {
            overview.put("totalQuestions", messageMapper.countTotalQuestions());
            overview.put("activeUsers", messageMapper.countActiveSessions());
            overview.put("avgResponseTime", 1.2);
            overview.put("citationRate", 85);
        } catch (Exception e) {
            overview.put("totalQuestions", 0);
            overview.put("activeUsers", 0);
            overview.put("avgResponseTime", 0);
            overview.put("citationRate", 0);
        }
        return overview;
    }

    public List<Map<String, Object>> getHotKeywords(int limit) {
        List<Map<String, Object>> keywords = new ArrayList<>();
        try {
            List<String> recentQuestions = messageMapper.findRecentQuestionContents(limit * 3);
            Map<String, Integer> wordCount = new HashMap<>();
            String[] osKeywords = {"进程", "线程", "死锁", "信号量", "内存", "页面置换",
                    "LRU", "调度", "文件系统", "中断", "同步", "互斥", "虚拟内存",
                    "分页", "分段", "缓冲", "管道", "套接字", "PV操作", "银行家算法"};

            for (String question : recentQuestions) {
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

    public List<Map<String, Object>> getQuestionTrend() {
        return new ArrayList<>();
    }

    public List<Map<String, Object>> getRecentQuestions(int limit) {
        return new ArrayList<>();
    }
}
