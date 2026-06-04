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

    public Map<String, Object> getOverview(String startDate, String endDate) {
        Map<String, Object> overview = new HashMap<>();
        try {
            if (startDate != null && endDate != null) {
                overview.put("totalQuestions", messageMapper.countTotalQuestionsByDate(startDate, endDate));
                overview.put("activeUsers", messageMapper.countActiveSessionsByDate(startDate, endDate));
                int totalAnswers = messageMapper.countTotalAnswersByDate(startDate, endDate);
                int citedAnswers = messageMapper.countCitedAnswersByDate(startDate, endDate);
                overview.put("citationRate", totalAnswers > 0 ? Math.round(citedAnswers * 100.0 / totalAnswers) : 0);
            } else {
                overview.put("totalQuestions", messageMapper.countTotalQuestions());
                overview.put("activeUsers", messageMapper.countActiveSessions());
                int totalAnswers = messageMapper.countTotalAnswers();
                int citedAnswers = messageMapper.countCitedAnswers();
                overview.put("citationRate", totalAnswers > 0 ? Math.round(citedAnswers * 100.0 / totalAnswers) : 0);
            }
            overview.put("avgResponseTime", 1.2);
        } catch (Exception e) {
            overview.put("totalQuestions", 0);
            overview.put("activeUsers", 0);
            overview.put("avgResponseTime", 0);
            overview.put("citationRate", 0);
        }
        return overview;
    }

    public List<Map<String, Object>> getHotKeywords(String startDate, String endDate, int limit) {
        List<Map<String, Object>> keywords = new ArrayList<>();
        try {
            List<String> questions;
            if (startDate != null && endDate != null) {
                questions = messageMapper.findQuestionContentsByDate(startDate, endDate, limit * 3);
            } else {
                questions = messageMapper.findRecentQuestionContents(limit * 3);
            }

            Map<String, Integer> wordCount = new HashMap<>();
            String[] osKeywords = {"进程", "线程", "死锁", "信号量", "内存", "页面置换",
                    "LRU", "调度", "文件系统", "中断", "同步", "互斥", "虚拟内存",
                    "分页", "分段", "缓冲", "管道", "套接字", "PV操作", "银行家算法",
                    "进程通信", "进程调度", "作业调度", "磁盘调度", "内存分配", "内存保护",
                    "抖动", "缺页", "快表", "位图", "空闲链表", "伙伴系统", "slab",
                    "索引节点", "目录", "超级块", "FAT", "RAID", "SPOOLing",
                    "用户态", "内核态", "系统调用", "上下文切换", "时间片", "优先级",
                    "临界区", "管程", "条件变量", "读写锁", "自旋锁", "屏障",
                    "生产者消费者", "读者写者", "哲学家就餐", "饥饿", "活锁",
                    "安全序列", "资源分配图", "抢占", "非抢占", "死锁避免", "死锁检测",
                    "死锁预防", "死锁恢复", "页表", "多级页表", "反置页表",
                    "TLB", "局部性原理", "工作集", "置换算法", "FIFO", "OPT", "CLOCK",
                    "覆盖", "交换", "虚拟页式", "实页式", "段页式",
                    "文件控制块", "文件目录", "空闲空间管理", "连续分配", "链接分配",
                    "索引分配", "磁盘结构", "寻道", "旋转延迟", "传输时间",
                    "I/O软件", "设备驱动", "中断处理", "DMA", "通道",
                    "进程控制块", "PCB", "进程状态", "就绪", "阻塞", "运行",
                    "fork", "exec", "wait", "exit", "守护进程", "僵尸进程"};

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

    public List<Map<String, Object>> getRecentQuestions(String startDate, String endDate, int limit) {
        try {
            if (startDate != null && endDate != null) {
                return messageMapper.findRecentQuestionsByDate(startDate, endDate, limit);
            }
            return messageMapper.findRecentQuestions(limit);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
