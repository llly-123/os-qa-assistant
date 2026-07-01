package com.xidian.osqa.service;

import com.xidian.osqa.mapper.ChatMessageMapper;
import com.xidian.osqa.mapper.ChatSessionMapper;
import com.xidian.osqa.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class StatisticsService {

    private static final Logger log = LoggerFactory.getLogger(StatisticsService.class);

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

    public Map<String, Object> getClassOverview(Long classId, String startDate, String endDate) {
        Map<String, Object> overview = new HashMap<>();
        try {
            Map<String, Object> raw;
            int totalAnswers, citedAnswers;
            if (startDate != null && endDate != null) {
                raw = messageMapper.findClassOverviewByDate(classId, startDate, endDate);
                totalAnswers = messageMapper.countClassTotalAnswersByDate(classId, startDate, endDate);
                citedAnswers = messageMapper.countClassCitedAnswersByDate(classId, startDate, endDate);
            } else {
                raw = messageMapper.findClassOverview(classId);
                totalAnswers = messageMapper.countClassTotalAnswers(classId);
                citedAnswers = messageMapper.countClassCitedAnswers(classId);
            }
            overview.put("totalQuestions", getMapInt(raw, "totalQuestions"));
            overview.put("activeUsers", getMapInt(raw, "activeUsers"));
            overview.put("citationRate", totalAnswers > 0 ? Math.round(citedAnswers * 100.0 / totalAnswers) : 0);
            overview.put("avgResponseTime", 1.2);
        } catch (Exception e) {
            log.error("获取班级概览失败, classId={}", classId, e);
            overview.put("totalQuestions", 0);
            overview.put("activeUsers", 0);
            overview.put("avgResponseTime", 0);
            overview.put("citationRate", 0);
        }
        return overview;
    }

    public List<Map<String, Object>> getClassHotKeywords(Long classId, String startDate, String endDate, int limit) {
        List<Map<String, Object>> keywords = new ArrayList<>();
        try {
            List<String> questions;
            if (startDate != null && endDate != null) {
                questions = messageMapper.findClassQuestionContentsByDate(classId, startDate, endDate, limit * 3);
            } else {
                questions = messageMapper.findClassQuestionContents(classId, limit * 3);
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
            log.error("获取班级关键词失败, classId={}", classId, e);
        }
        return keywords;
    }

    public List<Map<String, Object>> getClassList() {
        try {
            List<Map<String, Object>> list = messageMapper.findClassList();
            // H2数据库返回大写列名，统一转为驼峰命名
            Map<String, String> keyMap = new HashMap<>();
            keyMap.put("ID", "id");
            keyMap.put("NAME", "name");
            keyMap.put("STATUS", "status");
            keyMap.put("STARTTIME", "startTime");
            keyMap.put("ENDTIME", "endTime");
            keyMap.put("STUDENTCOUNT", "studentCount");
            for (Map<String, Object> row : list) {
                Map<String, Object> normalized = new LinkedHashMap<>();
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    String key = entry.getKey();
                    normalized.put(keyMap.getOrDefault(key, keyMap.getOrDefault(key.toUpperCase(), key)), entry.getValue());
                }
                row.clear();
                row.putAll(normalized);
            }
            return list;
        } catch (Exception e) {
            log.error("获取班级列表失败", e);
            return new ArrayList<>();
        }
    }

    public List<Map<String, Object>> getRecentQuestions(String startDate, String endDate, int limit) {
        try {
            List<Map<String, Object>> raw;
            if (startDate != null && endDate != null) {
                raw = messageMapper.findRecentQuestionsByDate(startDate, endDate, limit);
            } else {
                raw = messageMapper.findRecentQuestions(limit);
            }
            // 转换CLOB为String
            for (Map<String, Object> row : raw) {
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    if (entry.getValue() instanceof java.sql.Clob) {
                        try {
                            entry.setValue(((java.sql.Clob) entry.getValue()).getSubString(1, (int) ((java.sql.Clob) entry.getValue()).length()));
                        } catch (Exception e) {
                            entry.setValue(String.valueOf(entry.getValue()));
                        }
                    }
                }
            }
            return raw;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public List<Map<String, Object>> getUserRecentQuestions(Long userId, int limit) {
        try {
            List<Map<String, Object>> raw = messageMapper.findUserRecentQuestions(userId, limit);
            // 转换所有CLOB为String
            for (Map<String, Object> row : raw) {
                Map<String, Object> newRow = new LinkedHashMap<>();
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    Object value = entry.getValue();
                    if (value instanceof java.sql.Clob) {
                        try {
                            value = ((java.sql.Clob) value).getSubString(1, (int) ((java.sql.Clob) value).length());
                        } catch (Exception e) {
                            value = "[内容解析失败]";
                        }
                    }
                    newRow.put(entry.getKey(), value);
                }

                // 处理问题内容
                String questionText = null;
                Object q = newRow.get("question");
                if (q == null) q = newRow.get("QUESTION");
                if (q != null) {
                    questionText = q.toString().trim();
                    if (questionText.length() > 100) {
                        questionText = questionText.substring(0, 100) + "...";
                    }
                }
                newRow.put("question", questionText != null ? questionText : "[无内容]");
                newRow.put("QUESTION", questionText != null ? questionText : "[无内容]");

                // 判断是否与教材相关
                boolean isRelated = false;
                if (questionText != null) {
                    String lowerText = questionText.toLowerCase();
                    isRelated = lowerText.contains("进程") || lowerText.contains("线程") ||
                            lowerText.contains("内存") || lowerText.contains("文件") ||
                            lowerText.contains("死锁") || lowerText.contains("调度") ||
                            lowerText.contains("虚拟") || lowerText.contains("页面") ||
                            lowerText.contains("磁盘") || lowerText.contains("io") ||
                            lowerText.contains("操作系统") || lowerText.contains("os") ||
                            lowerText.contains("算法") || lowerText.contains("同步") ||
                            lowerText.contains("信号量") || lowerText.contains("管程");
                }
                newRow.put("isRelated", isRelated);

                row.clear();
                row.putAll(newRow);
            }
            return raw;
        } catch (Exception e) {
            log.error("获取用户提问记录失败", e);
            return new ArrayList<>();
        }
    }

    // H2数据库返回大写列名，兼容大小写读取Map中的Number值
    private int getMapInt(Map<String, Object> map, String key) {
        if (map == null) return 0;
        Object val = map.get(key);
        if (val == null) val = map.get(key.toUpperCase());
        if (val == null) val = map.get(key.toLowerCase());
        return val != null ? ((Number) val).intValue() : 0;
    }
}
