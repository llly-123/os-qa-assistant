package com.xidian.osqa.service;

import com.xidian.osqa.common.KeywordExtractor;
import com.xidian.osqa.mapper.ChatMessageMapper;
import com.xidian.osqa.mapper.ChatSessionMapper;
import com.xidian.osqa.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class StatisticsService {

    private static final Logger log = LoggerFactory.getLogger(StatisticsService.class);

    private final ChatMessageMapper messageMapper;
    private final ChatSessionMapper sessionMapper;
    private final UserMapper userMapper;
    private final KeywordAiService keywordAiService;

    public StatisticsService(ChatMessageMapper messageMapper, ChatSessionMapper sessionMapper, UserMapper userMapper, KeywordAiService keywordAiService) {
        this.messageMapper = messageMapper;
        this.sessionMapper = sessionMapper;
        this.userMapper = userMapper;
        this.keywordAiService = keywordAiService;
    }

    public Map<String, Object> getOverview(Long teacherId, String startDate, String endDate) {
        Map<String, Object> overview = new HashMap<>();
        try {
            if (startDate != null && endDate != null) {
                overview.put("totalQuestions", messageMapper.countTotalQuestionsByDate(teacherId, startDate, endDate));
                overview.put("activeUsers", messageMapper.countActiveSessionsByDate(teacherId, startDate, endDate));
                int totalAnswers = messageMapper.countTotalAnswersByDate(teacherId, startDate, endDate);
                int citedAnswers = messageMapper.countCitedAnswersByDate(teacherId, startDate, endDate);
                overview.put("citationRate", totalAnswers > 0 ? Math.round(citedAnswers * 100.0 / totalAnswers) : 0);
            } else {
                overview.put("totalQuestions", messageMapper.countTotalQuestions(teacherId));
                overview.put("activeUsers", messageMapper.countActiveSessions(teacherId));
                int totalAnswers = messageMapper.countTotalAnswers(teacherId);
                int citedAnswers = messageMapper.countCitedAnswers(teacherId);
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

    public List<Map<String, Object>> getHotKeywords(Long teacherId, String startDate, String endDate, int limit) {
        try {
            List<String> kwJson;
            if (startDate != null && endDate != null) {
                kwJson = messageMapper.findQuestionKeywordsByDate(teacherId, startDate, endDate, limit * 3);
            } else {
                kwJson = messageMapper.findRecentQuestionKeywords(teacherId, limit * 3);
            }
            return keywordAiService.aggregate(kwJson, limit);
        } catch (Exception e) {
            return new ArrayList<>();
        }
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
        try {
            List<String> kwJson;
            if (startDate != null && endDate != null) {
                kwJson = messageMapper.findClassQuestionKeywordsByDate(classId, startDate, endDate, limit * 3);
            } else {
                kwJson = messageMapper.findClassQuestionKeywords(classId, limit * 3);
            }
            return keywordAiService.aggregate(kwJson, limit);
        } catch (Exception e) {
            log.error("获取班级关键词失败, classId={}", classId, e);
            return new ArrayList<>();
        }
    }

    public List<Map<String, Object>> getClassList(Long teacherId) {
        try {
            List<Map<String, Object>> list = messageMapper.findClassList(teacherId);
            // H2数据库返回大写列名，统一转为驼峰命名
            Map<String, String> keyMap = new HashMap<>();
            keyMap.put("ID", "id");
            keyMap.put("NAME", "name");
            keyMap.put("STATUS", "status");
            keyMap.put("STARTTIME", "startTime");
            keyMap.put("ENDTIME", "endTime");
            keyMap.put("VIDEOSETID", "videoSetId");
            keyMap.put("KBID", "kbId");
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

    public List<Map<String, Object>> getRecentQuestions(Long teacherId, String startDate, String endDate, int limit) {
        try {
            List<Map<String, Object>> raw;
            if (startDate != null && endDate != null) {
                raw = messageMapper.findRecentQuestionsByDate(teacherId, startDate, endDate, limit);
            } else {
                raw = messageMapper.findRecentQuestions(teacherId, limit);
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

    public List<Map<String, Object>> getUserRecentQuestions(Long userId, Long teacherId, Long classId, int limit) {
        try {
            List<Map<String, Object>> raw = messageMapper.findUserRecentQuestions(userId, teacherId, classId, limit);
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

                // 是否与课程相关：按来源类型判断（textbook/web=相关，no_class=无关）。
                // 注意 user 消息的 citation 恒为空（citation 属于 assistant 回答），不能用它判断。
                Object sourceType = newRow.get("sourceType");
                if (sourceType == null) sourceType = newRow.get("SOURCE_TYPE");
                boolean isRelated = sourceType != null && !"no_class".equals(sourceType.toString());
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

    private int getMapInt(Map<String, Object> map, String key) {
        if (map == null) return 0;
        Object val = map.get(key);
        if (val == null) val = map.get(key.toUpperCase());
        if (val == null) val = map.get(key.toLowerCase());
        return val != null ? ((Number) val).intValue() : 0;
    }

    // ===== 新增统计维度 =====

    private String[] getDefaultDateRange() {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(29);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return new String[]{start.format(fmt) + " 00:00:00", end.format(fmt) + " 23:59:59"};
    }

    public Map<String, Object> getQuestionTrend(Long teacherId, String startDate, String endDate, String granularity) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (startDate == null || endDate == null) {
                String[] range = getDefaultDateRange();
                startDate = range[0];
                endDate = range[1];
            }
            List<Map<String, Object>> trend;
            if ("weekly".equals(granularity)) {
                trend = messageMapper.findWeeklyQuestionTrend(teacherId, startDate, endDate);
            } else {
                trend = messageMapper.findDailyQuestionTrend(teacherId, startDate, endDate);
            }
            result.put("trend", normalizeMapList(trend));
            result.put("granularity", granularity);
        } catch (Exception e) {
            log.error("获取提问趋势失败", e);
            result.put("trend", new ArrayList<>());
        }
        return result;
    }

    public Map<String, Object> getClassQuestionTrend(Long classId, String startDate, String endDate, String granularity) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (startDate == null || endDate == null) {
                String[] range = getDefaultDateRange();
                startDate = range[0];
                endDate = range[1];
            }
            List<Map<String, Object>> trend;
            if ("weekly".equals(granularity)) {
                trend = messageMapper.findClassWeeklyQuestionTrend(classId, startDate, endDate);
            } else {
                trend = messageMapper.findClassDailyQuestionTrend(classId, startDate, endDate);
            }
            result.put("trend", normalizeMapList(trend));
        } catch (Exception e) {
            log.error("获取班级提问趋势失败", e);
            result.put("trend", new ArrayList<>());
        }
        return result;
    }

    public Map<String, Object> getSessionRounds(Long teacherId, String startDate, String endDate, int limit) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (startDate == null || endDate == null) {
                String[] range = getDefaultDateRange();
                startDate = range[0];
                endDate = range[1];
            }
            List<Map<String, Object>> rounds = messageMapper.findSessionRounds(teacherId, startDate, endDate, limit);
            result.put("rounds", normalizeMapList(rounds));
            // 计算平均轮次
            int totalRounds = 0;
            for (Map<String, Object> r : rounds) {
                totalRounds += getMapInt(r, "rounds");
            }
            result.put("avgRounds", rounds.isEmpty() ? 0 : Math.round(totalRounds * 100.0 / rounds.size()) / 100.0);
            result.put("totalSessions", rounds.size());
        } catch (Exception e) {
            log.error("获取会话轮次失败", e);
            result.put("rounds", new ArrayList<>());
            result.put("avgRounds", 0);
            result.put("totalSessions", 0);
        }
        return result;
    }

    public Map<String, Object> getClassSessionRounds(Long classId, String startDate, String endDate, int limit) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (startDate == null || endDate == null) {
                String[] range = getDefaultDateRange();
                startDate = range[0];
                endDate = range[1];
            }
            List<Map<String, Object>> rounds = messageMapper.findClassSessionRounds(classId, startDate, endDate, limit);
            result.put("rounds", normalizeMapList(rounds));
            int totalRounds = 0;
            for (Map<String, Object> r : rounds) {
                totalRounds += getMapInt(r, "rounds");
            }
            result.put("avgRounds", rounds.isEmpty() ? 0 : Math.round(totalRounds * 100.0 / rounds.size()) / 100.0);
            result.put("totalSessions", rounds.size());
        } catch (Exception e) {
            log.error("获取班级会话轮次失败", e);
            result.put("rounds", new ArrayList<>());
            result.put("avgRounds", 0);
            result.put("totalSessions", 0);
        }
        return result;
    }

    public Map<String, Object> getSourceDistribution(Long teacherId, String startDate, String endDate) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (startDate == null || endDate == null) {
                String[] range = getDefaultDateRange();
                startDate = range[0];
                endDate = range[1];
            }
            List<Map<String, Object>> dist = messageMapper.findSourceTypeDistribution(teacherId, startDate, endDate);
            result.put("distribution", normalizeMapList(dist));
        } catch (Exception e) {
            log.error("获取来源分布失败", e);
            result.put("distribution", new ArrayList<>());
        }
        return result;
    }

    public Map<String, Object> getClassSourceDistribution(Long classId, String startDate, String endDate) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (startDate == null || endDate == null) {
                String[] range = getDefaultDateRange();
                startDate = range[0];
                endDate = range[1];
            }
            List<Map<String, Object>> dist = messageMapper.findClassSourceTypeDistribution(classId, startDate, endDate);
            result.put("distribution", normalizeMapList(dist));
        } catch (Exception e) {
            log.error("获取班级来源分布失败", e);
            result.put("distribution", new ArrayList<>());
        }
        return result;
    }

    public Map<String, Object> getActiveDaysStats(Long teacherId, String startDate, String endDate) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (startDate == null || endDate == null) {
                String[] range = getDefaultDateRange();
                startDate = range[0];
                endDate = range[1];
            }
            List<Map<String, Object>> stats = messageMapper.findActiveDaysStats(teacherId, startDate, endDate);
            List<Map<String, Object>> normalized = normalizeMapList(stats);
            // 计算平均活跃天数
            int totalDays = 0;
            for (Map<String, Object> s : normalized) {
                totalDays += getMapInt(s, "activeDays");
            }
            result.put("students", normalized);
            result.put("avgActiveDays", normalized.isEmpty() ? 0 : Math.round(totalDays * 100.0 / normalized.size()) / 100.0);
        } catch (Exception e) {
            log.error("获取活跃天数失败", e);
            result.put("students", new ArrayList<>());
            result.put("avgActiveDays", 0);
        }
        return result;
    }

    public Map<String, Object> getClassActiveDaysStats(Long classId, String startDate, String endDate) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (startDate == null || endDate == null) {
                String[] range = getDefaultDateRange();
                startDate = range[0];
                endDate = range[1];
            }
            List<Map<String, Object>> stats = messageMapper.findClassActiveDaysStats(classId, startDate, endDate);
            List<Map<String, Object>> normalized = normalizeMapList(stats);
            int totalDays = 0;
            for (Map<String, Object> s : normalized) {
                totalDays += getMapInt(s, "activeDays");
            }
            result.put("students", normalized);
            result.put("avgActiveDays", normalized.isEmpty() ? 0 : Math.round(totalDays * 100.0 / normalized.size()) / 100.0);
        } catch (Exception e) {
            log.error("获取班级活跃天数失败", e);
            result.put("students", new ArrayList<>());
            result.put("avgActiveDays", 0);
        }
        return result;
    }

    // H2返回大写列名，统一转为驼峰
    private List<Map<String, Object>> normalizeMapList(List<Map<String, Object>> list) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : list) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String key = entry.getKey();
                if (Character.isUpperCase(key.charAt(0))) {
                    key = key.toLowerCase();
                }
                normalized.put(key, entry.getValue());
            }
            result.add(normalized);
        }
        return result;
    }
}
