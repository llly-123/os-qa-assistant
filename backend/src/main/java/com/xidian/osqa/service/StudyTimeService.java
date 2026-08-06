package com.xidian.osqa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xidian.osqa.entity.StudyTime;
import com.xidian.osqa.mapper.StudyTimeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class StudyTimeService {

    private static final Logger log = LoggerFactory.getLogger(StudyTimeService.class);

    private final StudyTimeMapper studyTimeMapper;

    public StudyTimeService(StudyTimeMapper studyTimeMapper) {
        this.studyTimeMapper = studyTimeMapper;
    }

    /** 单次上报秒数上限（防止异常数据，正常60秒心跳上报） */
    private static final int MAX_REPORT_SECONDS = 1800;

    /**
     * 上报学习时长：累加到 (userId, classId, 当天) 的记录上。
     * 若不存在则插入新记录。
     */
    @Transactional
    public void report(Long userId, Long classId, int seconds) {
        if (userId == null || classId == null || seconds <= 0) return;
        if (seconds > MAX_REPORT_SECONDS) seconds = MAX_REPORT_SECONDS;

        LocalDate today = LocalDate.now();
        StudyTime existing = studyTimeMapper.selectOne(new LambdaQueryWrapper<StudyTime>()
                .eq(StudyTime::getUserId, userId)
                .eq(StudyTime::getClassId, classId)
                .eq(StudyTime::getStudyDate, today));

        if (existing != null) {
            existing.setTotalSeconds(existing.getTotalSeconds() + seconds);
            existing.setUpdateTime(LocalDateTime.now());
            studyTimeMapper.updateById(existing);
        } else {
            StudyTime st = new StudyTime();
            st.setUserId(userId);
            st.setClassId(classId);
            st.setStudyDate(today);
            st.setTotalSeconds(seconds);
            studyTimeMapper.insert(st);
        }
    }

    /** 学生个人学习时长（可按班级过滤，日期为空则统计全部） */
    public Map<String, Object> getUserStudySeconds(Long userId, Long classId, String startDate, String endDate) {
        Map<String, Object> result = new HashMap<>();
        try {
            int seconds = Optional.ofNullable(
                    studyTimeMapper.sumUserStudySeconds(userId, classId, toDateStr(startDate), toDateStr(endDate)))
                    .orElse(0);
            result.put("totalSeconds", seconds);
            result.put("totalMinutes", seconds / 60);
        } catch (Exception e) {
            log.error("获取学生学习时长失败, userId={}", userId, e);
            result.put("totalSeconds", 0);
            result.put("totalMinutes", 0);
        }
        return result;
    }

    /** 教师总体学习时长统计（该教师所有班级） */
    public Map<String, Object> getTeacherStudyTimeStats(Long teacherId, String startDate, String endDate) {
        Map<String, Object> result = new HashMap<>();
        try {
            String sd = toDateStr(startDate);
            String ed = toDateStr(endDate);
            int total = Optional.ofNullable(studyTimeMapper.sumTeacherStudySeconds(teacherId, sd, ed)).orElse(0);
            result.put("totalSeconds", total);
            result.put("students", normalizeMapList(studyTimeMapper.findTeacherStudyTimeStats(teacherId, sd, ed)));
        } catch (Exception e) {
            log.error("获取教师总体学习时长统计失败, teacherId={}", teacherId, e);
            result.put("totalSeconds", 0);
            result.put("students", new ArrayList<>());
        }
        return result;
    }

    /** 班级学习时长统计 */
    public Map<String, Object> getClassStudyTimeStats(Long classId, String startDate, String endDate) {
        Map<String, Object> result = new HashMap<>();
        try {
            String sd = toDateStr(startDate);
            String ed = toDateStr(endDate);
            int total = Optional.ofNullable(studyTimeMapper.sumClassStudySeconds(classId, sd, ed)).orElse(0);
            result.put("totalSeconds", total);
            result.put("students", normalizeMapList(studyTimeMapper.findClassStudyTimeStats(classId, sd, ed)));
        } catch (Exception e) {
            log.error("获取班级学习时长统计失败, classId={}", classId, e);
            result.put("totalSeconds", 0);
            result.put("students", new ArrayList<>());
        }
        return result;
    }

    /** 兼容 "yyyy-MM-dd HH:mm:ss" 与 "yyyy-MM-dd"，统一转成日期字符串 */
    private String toDateStr(String datetime) {
        if (datetime == null || datetime.isBlank()) return null;
        return datetime.length() >= 10 ? datetime.substring(0, 10) : datetime;
    }

    /** 教师总体学习时长趋势（用于提问趋势图叠加时长曲线） */
    public List<Map<String, Object>> getTeacherStudyTimeTrend(Long teacherId, String startDate, String endDate, String granularity) {
        try {
            String sd = toDateStr(startDate);
            String ed = toDateStr(endDate);
            List<Map<String, Object>> list = "weekly".equals(granularity)
                    ? studyTimeMapper.findTeacherWeeklyStudyTimeTrend(teacherId, sd, ed)
                    : studyTimeMapper.findTeacherDailyStudyTimeTrend(teacherId, sd, ed);
            return normalizeMapList(list);
        } catch (Exception e) {
            log.error("获取教师学习时长趋势失败, teacherId={}", teacherId, e);
            return new ArrayList<>();
        }
    }

    /** 班级学习时长趋势 */
    public List<Map<String, Object>> getClassStudyTimeTrend(Long classId, String startDate, String endDate, String granularity) {
        try {
            String sd = toDateStr(startDate);
            String ed = toDateStr(endDate);
            List<Map<String, Object>> list = "weekly".equals(granularity)
                    ? studyTimeMapper.findClassWeeklyStudyTimeTrend(classId, sd, ed)
                    : studyTimeMapper.findClassDailyStudyTimeTrend(classId, sd, ed);
            return normalizeMapList(list);
        } catch (Exception e) {
            log.error("获取班级学习时长趋势失败, classId={}", classId, e);
            return new ArrayList<>();
        }
    }

    /** 学生个人学习时长趋势 */
    public List<Map<String, Object>> getUserStudyTimeTrend(Long userId, Long classId, String startDate, String endDate, String granularity) {
        try {
            String sd = toDateStr(startDate);
            String ed = toDateStr(endDate);
            List<Map<String, Object>> list = "weekly".equals(granularity)
                    ? studyTimeMapper.findUserWeeklyStudyTimeTrend(userId, classId, sd, ed)
                    : studyTimeMapper.findUserDailyStudyTimeTrend(userId, classId, sd, ed);
            return normalizeMapList(list);
        } catch (Exception e) {
            log.error("获取学生学习时长趋势失败, userId={}", userId, e);
            return new ArrayList<>();
        }
    }

    /** 学生每日学习时长（活跃天数热力图标志用，可按班级过滤） */
    public Map<String, Object> getUserDailyStudySeconds(Long userId, Long classId, String startDate, String endDate) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> rows = studyTimeMapper.findUserDailyStudySeconds(userId, classId, toDateStr(startDate), toDateStr(endDate));
            Map<String, Object> studyByDate = new LinkedHashMap<>();
            for (Map<String, Object> row : rows) {
                Object date = row.get("date");
                if (date == null) date = row.get("DATE");
                Object seconds = row.get("studySeconds");
                if (seconds == null) seconds = row.get("STUDYSECONDS");
                if (date != null && seconds != null) {
                    studyByDate.put(date.toString(), ((Number) seconds).intValue());
                }
            }
            result.put("studyByDate", studyByDate);
        } catch (Exception e) {
            log.error("获取学生每日学习时长失败, userId={}", userId, e);
            result.put("studyByDate", new HashMap<>());
        }
        return result;
    }

    // H2 返回大写列名（如 TOTALSECONDS），统一转为前端使用的小驼峰键
    private static final Map<String, String> COLUMN_MAP = new HashMap<>();
    static {
        COLUMN_MAP.put("USERID", "userId");
        COLUMN_MAP.put("USERNAME", "username");
        COLUMN_MAP.put("REALNAME", "realName");
        COLUMN_MAP.put("CLASSID", "classId");
        COLUMN_MAP.put("CLASSNAME", "className");
        COLUMN_MAP.put("TOTALSECONDS", "totalSeconds");
        COLUMN_MAP.put("LASTSTUDY", "lastStudy");
        COLUMN_MAP.put("DATE", "date");
        COLUMN_MAP.put("WEEK", "week");
        COLUMN_MAP.put("STUDYSECONDS", "studySeconds");
        COLUMN_MAP.put("SESSIONID", "sessionId");
        COLUMN_MAP.put("ROUNDS", "rounds");
        COLUMN_MAP.put("STARTTIME", "startTime");
        COLUMN_MAP.put("ENDTIME", "endTime");
        COLUMN_MAP.put("SESSIONSECONDS", "sessionSeconds");
        COLUMN_MAP.put("COUNT", "count");
        COLUMN_MAP.put("ACTIVEDAYS", "activeDays");
        COLUMN_MAP.put("LASTACTIVE", "lastActive");
        COLUMN_MAP.put("SOURCE", "source");
        COLUMN_MAP.put("QUESTION", "question");
        COLUMN_MAP.put("STUDENTNAME", "studentName");
        COLUMN_MAP.put("STUDENTREALNAME", "studentRealName");
        COLUMN_MAP.put("TOTALQUESTIONS", "totalQuestions");
        COLUMN_MAP.put("ACTIVEUSERS", "activeUsers");
        COLUMN_MAP.put("LASTQUESTIONTIME", "lastQuestionTime");
        COLUMN_MAP.put("ID", "id");
        COLUMN_MAP.put("NAME", "name");
        COLUMN_MAP.put("STATUS", "status");
        COLUMN_MAP.put("VIDEOSETID", "videoSetId");
        COLUMN_MAP.put("KBID", "kbId");
        COLUMN_MAP.put("STUDENTCOUNT", "studentCount");
    }

    private List<Map<String, Object>> normalizeMapList(List<Map<String, Object>> list) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : list) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String key = entry.getKey();
                String mapped = COLUMN_MAP.get(key);
                if (mapped == null && !key.isEmpty() && Character.isUpperCase(key.charAt(0))) {
                    mapped = key.toLowerCase();
                }
                normalized.put(mapped != null ? mapped : key, entry.getValue());
            }
            result.add(normalized);
        }
        return result;
    }
}
