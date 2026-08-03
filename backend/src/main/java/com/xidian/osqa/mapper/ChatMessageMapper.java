package com.xidian.osqa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xidian.osqa.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    // ===== 总览维度（按教师作用域：只统计该教师班级下的会话）=====

    @Select("SELECT COUNT(*) FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "JOIN clazz c ON c.id = s.class_id AND c.deleted = 0 AND c.teacher_id = #{teacherId} " +
            "WHERE m.role = 'user'")
    int countTotalQuestions(@Param("teacherId") Long teacherId);

    @Select("SELECT COUNT(*) FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "JOIN clazz c ON c.id = s.class_id AND c.deleted = 0 AND c.teacher_id = #{teacherId} " +
            "WHERE m.role = 'user' AND m.create_time >= #{startDate} AND m.create_time <= #{endDate}")
    int countTotalQuestionsByDate(@Param("teacherId") Long teacherId, @Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("SELECT COUNT(DISTINCT s.user_id) FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "JOIN clazz c ON c.id = s.class_id AND c.deleted = 0 AND c.teacher_id = #{teacherId} " +
            "WHERE m.role = 'user'")
    int countActiveSessions(@Param("teacherId") Long teacherId);

    @Select("SELECT COUNT(DISTINCT s.user_id) FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "JOIN clazz c ON c.id = s.class_id AND c.deleted = 0 AND c.teacher_id = #{teacherId} " +
            "WHERE m.role = 'user' AND m.create_time >= #{startDate} AND m.create_time <= #{endDate}")
    int countActiveSessionsByDate(@Param("teacherId") Long teacherId, @Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("SELECT m.content FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "JOIN clazz c ON c.id = s.class_id AND c.deleted = 0 AND c.teacher_id = #{teacherId} " +
            "WHERE m.role = 'user' ORDER BY m.create_time DESC LIMIT #{limit}")
    List<String> findRecentQuestionContents(@Param("teacherId") Long teacherId, @Param("limit") int limit);

    @Select("SELECT m.content FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "JOIN clazz c ON c.id = s.class_id AND c.deleted = 0 AND c.teacher_id = #{teacherId} " +
            "WHERE m.role = 'user' AND m.create_time >= #{startDate} AND m.create_time <= #{endDate} " +
            "ORDER BY m.create_time DESC LIMIT #{limit}")
    List<String> findQuestionContentsByDate(@Param("teacherId") Long teacherId, @Param("startDate") String startDate, @Param("endDate") String endDate, @Param("limit") int limit);

    @Select("SELECT m.id, m.content as question, m.create_time, m.citation, m.source_type, " +
            "u.username as studentName, u.real_name as studentRealName " +
            "FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "JOIN clazz c ON c.id = s.class_id AND c.deleted = 0 AND c.teacher_id = #{teacherId} " +
            "LEFT JOIN sys_user u ON s.user_id = u.id AND (u.deleted = 0 OR u.deleted IS NULL) " +
            "WHERE m.role = 'user' " +
            "ORDER BY m.create_time DESC LIMIT #{limit}")
    List<Map<String, Object>> findRecentQuestions(@Param("teacherId") Long teacherId, @Param("limit") int limit);

    @Select("SELECT m.id, m.content as question, m.create_time, m.citation, m.source_type, " +
            "u.username as studentName, u.real_name as studentRealName " +
            "FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "JOIN clazz c ON c.id = s.class_id AND c.deleted = 0 AND c.teacher_id = #{teacherId} " +
            "LEFT JOIN sys_user u ON s.user_id = u.id AND (u.deleted = 0 OR u.deleted IS NULL) " +
            "WHERE m.role = 'user' AND m.create_time >= #{startDate} AND m.create_time <= #{endDate} " +
            "ORDER BY m.create_time DESC LIMIT #{limit}")
    List<Map<String, Object>> findRecentQuestionsByDate(@Param("teacherId") Long teacherId, @Param("startDate") String startDate, @Param("endDate") String endDate, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "JOIN clazz c ON c.id = s.class_id AND c.deleted = 0 AND c.teacher_id = #{teacherId} " +
            "WHERE m.role = 'assistant' AND m.citation IS NOT NULL AND m.citation != ''")
    int countCitedAnswers(@Param("teacherId") Long teacherId);

    @Select("SELECT COUNT(*) FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "JOIN clazz c ON c.id = s.class_id AND c.deleted = 0 AND c.teacher_id = #{teacherId} " +
            "WHERE m.role = 'assistant' AND m.citation IS NOT NULL AND m.citation != '' AND m.create_time >= #{startDate} AND m.create_time <= #{endDate}")
    int countCitedAnswersByDate(@Param("teacherId") Long teacherId, @Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("SELECT COUNT(*) FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "JOIN clazz c ON c.id = s.class_id AND c.deleted = 0 AND c.teacher_id = #{teacherId} " +
            "WHERE m.role = 'assistant'")
    int countTotalAnswers(@Param("teacherId") Long teacherId);

    @Select("SELECT COUNT(*) FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "JOIN clazz c ON c.id = s.class_id AND c.deleted = 0 AND c.teacher_id = #{teacherId} " +
            "WHERE m.role = 'assistant' AND m.create_time >= #{startDate} AND m.create_time <= #{endDate}")
    int countTotalAnswersByDate(@Param("teacherId") Long teacherId, @Param("startDate") String startDate, @Param("endDate") String endDate);

    // ===== 用户维度（学生个人统计，可按班级过滤）=====

    @Select("SELECT m.content FROM chat_message m " +
            "LEFT JOIN chat_session s ON m.session_id = s.id " +
            "WHERE m.role = 'user' AND s.user_id = #{userId} " +
            "ORDER BY m.create_time DESC LIMIT #{limit}")
    List<String> findUserQuestionContents(@Param("userId") Long userId, @Param("limit") int limit);

    @Select("SELECT m.content FROM chat_message m " +
            "LEFT JOIN chat_session s ON m.session_id = s.id " +
            "WHERE m.role = 'user' AND s.user_id = #{userId} AND s.class_id = #{classId} " +
            "ORDER BY m.create_time DESC LIMIT #{limit}")
    List<String> findUserQuestionContentsByClass(@Param("userId") Long userId, @Param("classId") Long classId, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM chat_message m " +
            "LEFT JOIN chat_session s ON m.session_id = s.id " +
            "WHERE m.role = 'user' AND s.user_id = #{userId} AND (m.source_type IS NULL OR m.source_type != 'no_class')")
    int countUserQuestions(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM chat_message m " +
            "LEFT JOIN chat_session s ON m.session_id = s.id " +
            "WHERE m.role = 'user' AND s.user_id = #{userId} AND s.class_id = #{classId} AND (m.source_type IS NULL OR m.source_type != 'no_class')")
    int countUserQuestionsByClass(@Param("userId") Long userId, @Param("classId") Long classId);

    @Select("SELECT COUNT(DISTINCT m.session_id) FROM chat_message m " +
            "LEFT JOIN chat_session s ON m.session_id = s.id " +
            "WHERE m.role = 'assistant' AND s.user_id = #{userId} AND m.citation IS NOT NULL AND m.citation != '' AND (m.source_type IS NULL OR m.source_type != 'no_class')")
    int countUserCitedAnswers(@Param("userId") Long userId);

    @Select("SELECT COUNT(DISTINCT m.session_id) FROM chat_message m " +
            "LEFT JOIN chat_session s ON m.session_id = s.id " +
            "WHERE m.role = 'assistant' AND s.user_id = #{userId} AND s.class_id = #{classId} AND m.citation IS NOT NULL AND m.citation != '' AND (m.source_type IS NULL OR m.source_type != 'no_class')")
    int countUserCitedAnswersByClass(@Param("userId") Long userId, @Param("classId") Long classId);

    @Select("SELECT COUNT(*) FROM chat_message m " +
            "LEFT JOIN chat_session s ON m.session_id = s.id " +
            "WHERE m.role = 'assistant' AND s.user_id = #{userId} AND (m.source_type IS NULL OR m.source_type != 'no_class')")
    int countUserTotalAnswers(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM chat_message m " +
            "LEFT JOIN chat_session s ON m.session_id = s.id " +
            "WHERE m.role = 'assistant' AND s.user_id = #{userId} AND s.class_id = #{classId} AND (m.source_type IS NULL OR m.source_type != 'no_class')")
    int countUserTotalAnswersByClass(@Param("userId") Long userId, @Param("classId") Long classId);

    @Select("<script>SELECT m.id, m.content as question, m.create_time, m.citation, m.source_type " +
            "FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "<if test='classId != null'>JOIN clazz c ON c.id = s.class_id AND c.deleted = 0 AND c.teacher_id = #{teacherId} </if>" +
            "<if test='classId == null'>LEFT JOIN clazz c ON c.id = s.class_id AND c.deleted = 0 AND c.teacher_id = #{teacherId} </if>" +
            "WHERE m.role = 'user' AND s.user_id = #{userId} " +
            "<if test='classId != null'>AND s.class_id = #{classId}</if> " +
            "<if test='classId == null'>AND (c.teacher_id = #{teacherId} OR s.class_id IS NULL)</if> " +
            "ORDER BY m.create_time DESC LIMIT #{limit}</script>")
    List<Map<String, Object>> findUserRecentQuestions(@Param("userId") Long userId, @Param("teacherId") Long teacherId, @Param("classId") Long classId, @Param("limit") int limit);

    @Select("SELECT s.user_id as userId, MAX(m.create_time) as lastQuestionTime " +
            "FROM chat_message m " +
            "LEFT JOIN chat_session s ON m.session_id = s.id " +
            "WHERE m.role = 'user' " +
            "GROUP BY s.user_id")
    List<Map<String, Object>> findAllLastQuestionTimes();

    // ===== 班级维度统计（按 chat_session.class_id 归属，避免多班级学生重复计数）=====

    @Select("SELECT c.id as id, c.name as name, c.status as status, c.start_time as startTime, c.end_time as endTime, " +
            "c.video_set_id as videoSetId, c.kb_id as kbId, " +
            "COUNT(DISTINCT CASE WHEN u.deleted = 0 THEN cs.student_id END) as studentCount " +
            "FROM clazz c " +
            "LEFT JOIN class_student cs ON cs.class_id = c.id " +
            "LEFT JOIN sys_user u ON u.id = cs.student_id " +
            "WHERE c.deleted = 0 AND c.teacher_id = #{teacherId} " +
            "GROUP BY c.id, c.name, c.status, c.start_time, c.end_time, c.video_set_id, c.kb_id " +
            "ORDER BY c.status DESC, c.id DESC")
    List<Map<String, Object>> findClassList(@Param("teacherId") Long teacherId);

    @Select("SELECT COUNT(DISTINCT m.id) as totalQuestions, " +
            "COUNT(DISTINCT s.user_id) as activeUsers " +
            "FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "WHERE m.role = 'user' AND s.class_id = #{classId}")
    Map<String, Object> findClassOverview(@Param("classId") Long classId);

    @Select("SELECT COUNT(DISTINCT m.id) as totalQuestions, " +
            "COUNT(DISTINCT s.user_id) as activeUsers " +
            "FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "WHERE m.role = 'user' AND s.class_id = #{classId} AND m.create_time >= #{startDate} AND m.create_time <= #{endDate}")
    Map<String, Object> findClassOverviewByDate(@Param("classId") Long classId, @Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("SELECT COUNT(*) FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "WHERE m.role = 'assistant' AND m.citation IS NOT NULL AND m.citation != '' AND s.class_id = #{classId}")
    int countClassCitedAnswers(@Param("classId") Long classId);

    @Select("SELECT COUNT(*) FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "WHERE m.role = 'assistant' AND m.citation IS NOT NULL AND m.citation != '' AND s.class_id = #{classId} AND m.create_time >= #{startDate} AND m.create_time <= #{endDate}")
    int countClassCitedAnswersByDate(@Param("classId") Long classId, @Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("SELECT COUNT(*) FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "WHERE m.role = 'assistant' AND s.class_id = #{classId}")
    int countClassTotalAnswers(@Param("classId") Long classId);

    @Select("SELECT COUNT(*) FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "WHERE m.role = 'assistant' AND s.class_id = #{classId} AND m.create_time >= #{startDate} AND m.create_time <= #{endDate}")
    int countClassTotalAnswersByDate(@Param("classId") Long classId, @Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("SELECT m.content FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "WHERE m.role = 'user' AND s.class_id = #{classId} " +
            "ORDER BY m.create_time DESC LIMIT #{limit}")
    List<String> findClassQuestionContents(@Param("classId") Long classId, @Param("limit") int limit);

    @Select("SELECT m.content FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "WHERE m.role = 'user' AND s.class_id = #{classId} AND m.create_time >= #{startDate} AND m.create_time <= #{endDate} " +
            "ORDER BY m.create_time DESC LIMIT #{limit}")
    List<String> findClassQuestionContentsByDate(@Param("classId") Long classId, @Param("startDate") String startDate, @Param("endDate") String endDate, @Param("limit") int limit);

    // ===== 关键词字段查询（AI 提取后存 keywords，统计页直接聚合）=====

    @Select("SELECT m.keywords FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "JOIN clazz c ON c.id = s.class_id AND c.deleted = 0 AND c.teacher_id = #{teacherId} " +
            "WHERE m.role = 'user' AND m.keywords IS NOT NULL AND m.keywords <> '' " +
            "ORDER BY m.create_time DESC LIMIT #{limit}")
    List<String> findRecentQuestionKeywords(@Param("teacherId") Long teacherId, @Param("limit") int limit);

    @Select("SELECT m.keywords FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "JOIN clazz c ON c.id = s.class_id AND c.deleted = 0 AND c.teacher_id = #{teacherId} " +
            "WHERE m.role = 'user' AND m.keywords IS NOT NULL AND m.keywords <> '' " +
            "AND m.create_time >= #{startDate} AND m.create_time <= #{endDate} " +
            "ORDER BY m.create_time DESC LIMIT #{limit}")
    List<String> findQuestionKeywordsByDate(@Param("teacherId") Long teacherId, @Param("startDate") String startDate, @Param("endDate") String endDate, @Param("limit") int limit);

    @Select("SELECT m.keywords FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "WHERE m.role = 'user' AND s.class_id = #{classId} AND m.keywords IS NOT NULL AND m.keywords <> '' " +
            "ORDER BY m.create_time DESC LIMIT #{limit}")
    List<String> findClassQuestionKeywords(@Param("classId") Long classId, @Param("limit") int limit);

    @Select("SELECT m.keywords FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "WHERE m.role = 'user' AND s.class_id = #{classId} AND m.keywords IS NOT NULL AND m.keywords <> '' " +
            "AND m.create_time >= #{startDate} AND m.create_time <= #{endDate} " +
            "ORDER BY m.create_time DESC LIMIT #{limit}")
    List<String> findClassQuestionKeywordsByDate(@Param("classId") Long classId, @Param("startDate") String startDate, @Param("endDate") String endDate, @Param("limit") int limit);

    @Select("SELECT m.keywords FROM chat_message m " +
            "LEFT JOIN chat_session s ON m.session_id = s.id " +
            "WHERE m.role = 'user' AND s.user_id = #{userId} AND m.keywords IS NOT NULL AND m.keywords <> '' AND (m.source_type IS NULL OR m.source_type != 'no_class') " +
            "ORDER BY m.create_time DESC LIMIT #{limit}")
    List<String> findUserQuestionKeywords(@Param("userId") Long userId, @Param("limit") int limit);

    @Select("SELECT m.keywords FROM chat_message m " +
            "LEFT JOIN chat_session s ON m.session_id = s.id " +
            "WHERE m.role = 'user' AND s.user_id = #{userId} AND s.class_id = #{classId} AND m.keywords IS NOT NULL AND m.keywords <> '' AND (m.source_type IS NULL OR m.source_type != 'no_class') " +
            "ORDER BY m.create_time DESC LIMIT #{limit}")
    List<String> findUserQuestionKeywordsByClass(@Param("userId") Long userId, @Param("classId") Long classId, @Param("limit") int limit);

    // ===== 新增统计维度 =====

    // 每日提问趋势（教师维度）
    @Select("SELECT FORMATDATETIME(m.create_time, 'yyyy-MM-dd') as date, COUNT(*) as count " +
            "FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "JOIN clazz c ON c.id = s.class_id AND c.deleted = 0 AND c.teacher_id = #{teacherId} " +
            "WHERE m.role = 'user' AND m.create_time >= #{startDate} AND m.create_time <= #{endDate} " +
            "GROUP BY FORMATDATETIME(m.create_time, 'yyyy-MM-dd') " +
            "ORDER BY date")
    List<Map<String, Object>> findDailyQuestionTrend(@Param("teacherId") Long teacherId, @Param("startDate") String startDate, @Param("endDate") String endDate);

    // 每日提问趋势（班级维度）
    @Select("SELECT FORMATDATETIME(m.create_time, 'yyyy-MM-dd') as date, COUNT(*) as count " +
            "FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "WHERE m.role = 'user' AND s.class_id = #{classId} AND m.create_time >= #{startDate} AND m.create_time <= #{endDate} " +
            "GROUP BY FORMATDATETIME(m.create_time, 'yyyy-MM-dd') " +
            "ORDER BY date")
    List<Map<String, Object>> findClassDailyQuestionTrend(@Param("classId") Long classId, @Param("startDate") String startDate, @Param("endDate") String endDate);

    // 每日提问趋势（学生维度）
    @Select("SELECT FORMATDATETIME(m.create_time, 'yyyy-MM-dd') as date, COUNT(*) as count " +
            "FROM chat_message m " +
            "LEFT JOIN chat_session s ON m.session_id = s.id " +
            "WHERE m.role = 'user' AND s.user_id = #{userId} AND (m.source_type IS NULL OR m.source_type != 'no_class') " +
            "AND m.create_time >= #{startDate} AND m.create_time <= #{endDate} " +
            "GROUP BY FORMATDATETIME(m.create_time, 'yyyy-MM-dd') " +
            "ORDER BY date")
    List<Map<String, Object>> findUserDailyQuestionTrend(@Param("userId") Long userId, @Param("startDate") String startDate, @Param("endDate") String endDate);

    // 每周提问趋势（教师维度）- weekStart为该周内最早日期
    @Select("SELECT FORMATDATETIME(MIN(CAST(m.create_time AS DATE)), 'yyyy-MM-dd') as week, COUNT(*) as count " +
            "FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "JOIN clazz c ON c.id = s.class_id AND c.deleted = 0 AND c.teacher_id = #{teacherId} " +
            "WHERE m.role = 'user' AND m.create_time >= #{startDate} AND m.create_time <= #{endDate} " +
            "GROUP BY YEAR(m.create_time), WEEK(m.create_time) " +
            "ORDER BY week")
    List<Map<String, Object>> findWeeklyQuestionTrend(@Param("teacherId") Long teacherId, @Param("startDate") String startDate, @Param("endDate") String endDate);

    // 每周提问趋势（班级维度）
    @Select("SELECT FORMATDATETIME(MIN(CAST(m.create_time AS DATE)), 'yyyy-MM-dd') as week, COUNT(*) as count " +
            "FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "WHERE m.role = 'user' AND s.class_id = #{classId} AND m.create_time >= #{startDate} AND m.create_time <= #{endDate} " +
            "GROUP BY YEAR(m.create_time), WEEK(m.create_time) " +
            "ORDER BY week")
    List<Map<String, Object>> findClassWeeklyQuestionTrend(@Param("classId") Long classId, @Param("startDate") String startDate, @Param("endDate") String endDate);

    // 每周提问趋势（学生维度）
    @Select("SELECT FORMATDATETIME(MIN(CAST(m.create_time AS DATE)), 'yyyy-MM-dd') as week, COUNT(*) as count " +
            "FROM chat_message m " +
            "LEFT JOIN chat_session s ON m.session_id = s.id " +
            "WHERE m.role = 'user' AND s.user_id = #{userId} AND (m.source_type IS NULL OR m.source_type != 'no_class') " +
            "AND m.create_time >= #{startDate} AND m.create_time <= #{endDate} " +
            "GROUP BY YEAR(m.create_time), WEEK(m.create_time) " +
            "ORDER BY week")
    List<Map<String, Object>> findUserWeeklyQuestionTrend(@Param("userId") Long userId, @Param("startDate") String startDate, @Param("endDate") String endDate);

    // 会话轮次统计（教师维度）- 每个会话的问答轮次
    @Select("SELECT s.id as sessionId, COUNT(m.id) as rounds, s.user_id as userId, u.username as username, u.real_name as realName, " +
            "MIN(m.create_time) as startTime, MAX(m.create_time) as endTime " +
            "FROM chat_session s " +
            "JOIN clazz c ON c.id = s.class_id AND c.deleted = 0 AND c.teacher_id = #{teacherId} " +
            "JOIN chat_message m ON m.session_id = s.id AND s.deleted = 0 " +
            "LEFT JOIN sys_user u ON s.user_id = u.id " +
            "WHERE m.role = 'user' AND m.create_time >= #{startDate} AND m.create_time <= #{endDate} " +
            "GROUP BY s.id, s.user_id, u.username, u.real_name " +
            "ORDER BY rounds DESC LIMIT #{limit}")
    List<Map<String, Object>> findSessionRounds(@Param("teacherId") Long teacherId, @Param("startDate") String startDate, @Param("endDate") String endDate, @Param("limit") int limit);

    // 会话轮次统计（班级维度）
    @Select("SELECT s.id as sessionId, COUNT(m.id) as rounds, s.user_id as userId, u.username as username, u.real_name as realName, " +
            "MIN(m.create_time) as startTime, MAX(m.create_time) as endTime " +
            "FROM chat_session s " +
            "JOIN chat_message m ON m.session_id = s.id AND s.deleted = 0 " +
            "LEFT JOIN sys_user u ON s.user_id = u.id " +
            "WHERE m.role = 'user' AND s.class_id = #{classId} AND m.create_time >= #{startDate} AND m.create_time <= #{endDate} " +
            "GROUP BY s.id, s.user_id, u.username, u.real_name " +
            "ORDER BY rounds DESC LIMIT #{limit}")
    List<Map<String, Object>> findClassSessionRounds(@Param("classId") Long classId, @Param("startDate") String startDate, @Param("endDate") String endDate, @Param("limit") int limit);

    // 会话轮次统计（学生维度）
    @Select("SELECT s.id as sessionId, COUNT(m.id) as rounds, " +
            "MIN(m.create_time) as startTime, MAX(m.create_time) as endTime " +
            "FROM chat_session s " +
            "JOIN chat_message m ON m.session_id = s.id AND s.deleted = 0 " +
            "WHERE m.role = 'user' AND s.user_id = #{userId} AND (m.source_type IS NULL OR m.source_type != 'no_class') " +
            "AND m.create_time >= #{startDate} AND m.create_time <= #{endDate} " +
            "GROUP BY s.id " +
            "ORDER BY rounds DESC LIMIT #{limit}")
    List<Map<String, Object>> findUserSessionRounds(@Param("userId") Long userId, @Param("startDate") String startDate, @Param("endDate") String endDate, @Param("limit") int limit);

    // 提问来源分布（教师维度）
    @Select("SELECT COALESCE(m.source_type, 'unknown') as source, COUNT(*) as count " +
            "FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "JOIN clazz c ON c.id = s.class_id AND c.deleted = 0 AND c.teacher_id = #{teacherId} " +
            "WHERE m.role = 'user' AND m.create_time >= #{startDate} AND m.create_time <= #{endDate} " +
            "GROUP BY COALESCE(m.source_type, 'unknown')")
    List<Map<String, Object>> findSourceTypeDistribution(@Param("teacherId") Long teacherId, @Param("startDate") String startDate, @Param("endDate") String endDate);

    // 提问来源分布（班级维度）
    @Select("SELECT COALESCE(m.source_type, 'unknown') as source, COUNT(*) as count " +
            "FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "WHERE m.role = 'user' AND s.class_id = #{classId} AND m.create_time >= #{startDate} AND m.create_time <= #{endDate} " +
            "GROUP BY COALESCE(m.source_type, 'unknown')")
    List<Map<String, Object>> findClassSourceTypeDistribution(@Param("classId") Long classId, @Param("startDate") String startDate, @Param("endDate") String endDate);

    // 提问来源分布（学生维度）
    @Select("SELECT COALESCE(m.source_type, 'unknown') as source, COUNT(*) as count " +
            "FROM chat_message m " +
            "LEFT JOIN chat_session s ON m.session_id = s.id " +
            "WHERE m.role = 'user' AND s.user_id = #{userId} " +
            "AND m.create_time >= #{startDate} AND m.create_time <= #{endDate} " +
            "GROUP BY COALESCE(m.source_type, 'unknown')")
    List<Map<String, Object>> findUserSourceTypeDistribution(@Param("userId") Long userId, @Param("startDate") String startDate, @Param("endDate") String endDate);

    // 活跃天数统计（教师维度）- 统计每个学生的活跃天数和连续学习天数
    @Select("SELECT s.user_id as userId, u.username as username, u.real_name as realName, " +
            "COUNT(DISTINCT FORMATDATETIME(m.create_time, 'yyyy-MM-dd')) as activeDays, " +
            "MAX(m.create_time) as lastActive " +
            "FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "JOIN clazz c ON c.id = s.class_id AND c.deleted = 0 AND c.teacher_id = #{teacherId} " +
            "LEFT JOIN sys_user u ON s.user_id = u.id " +
            "WHERE m.role = 'user' AND m.create_time >= #{startDate} AND m.create_time <= #{endDate} " +
            "GROUP BY s.user_id, u.username, u.real_name " +
            "ORDER BY activeDays DESC")
    List<Map<String, Object>> findActiveDaysStats(@Param("teacherId") Long teacherId, @Param("startDate") String startDate, @Param("endDate") String endDate);

    // 活跃天数统计（班级维度）
    @Select("SELECT s.user_id as userId, u.username as username, u.real_name as realName, " +
            "COUNT(DISTINCT FORMATDATETIME(m.create_time, 'yyyy-MM-dd')) as activeDays, " +
            "MAX(m.create_time) as lastActive " +
            "FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "LEFT JOIN sys_user u ON s.user_id = u.id " +
            "WHERE m.role = 'user' AND s.class_id = #{classId} AND m.create_time >= #{startDate} AND m.create_time <= #{endDate} " +
            "GROUP BY s.user_id, u.username, u.real_name " +
            "ORDER BY activeDays DESC")
    List<Map<String, Object>> findClassActiveDaysStats(@Param("classId") Long classId, @Param("startDate") String startDate, @Param("endDate") String endDate);

    // 学生个人活跃天数
    @Select("SELECT COUNT(DISTINCT FORMATDATETIME(m.create_time, 'yyyy-MM-dd')) as activeDays " +
            "FROM chat_message m " +
            "LEFT JOIN chat_session s ON m.session_id = s.id " +
            "WHERE m.role = 'user' AND s.user_id = #{userId} AND (m.source_type IS NULL OR m.source_type != 'no_class') " +
            "AND m.create_time >= #{startDate} AND m.create_time <= #{endDate}")
    int findUserActiveDays(@Param("userId") Long userId, @Param("startDate") String startDate, @Param("endDate") String endDate);

    // 学生每日提问明细（用于计算连续天数）
    @Select("SELECT DISTINCT FORMATDATETIME(m.create_time, 'yyyy-MM-dd') as date " +
            "FROM chat_message m " +
            "LEFT JOIN chat_session s ON m.session_id = s.id " +
            "WHERE m.role = 'user' AND s.user_id = #{userId} AND (m.source_type IS NULL OR m.source_type != 'no_class') " +
            "AND m.create_time >= #{startDate} AND m.create_time <= #{endDate} " +
            "ORDER BY date DESC")
    List<String> findUserActiveDates(@Param("userId") Long userId, @Param("startDate") String startDate, @Param("endDate") String endDate);
}
