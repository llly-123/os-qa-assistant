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

    @Select("SELECT COUNT(*) FROM chat_message WHERE role = 'user'")
    int countTotalQuestions();

    @Select("SELECT COUNT(*) FROM chat_message WHERE role = 'user' AND create_time >= #{startDate} AND create_time <= #{endDate}")
    int countTotalQuestionsByDate(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("SELECT COUNT(DISTINCT session_id) FROM chat_message WHERE role = 'user'")
    int countActiveSessions();

    @Select("SELECT COUNT(DISTINCT session_id) FROM chat_message WHERE role = 'user' AND create_time >= #{startDate} AND create_time <= #{endDate}")
    int countActiveSessionsByDate(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("SELECT content FROM chat_message WHERE role = 'user' ORDER BY create_time DESC LIMIT #{limit}")
    List<String> findRecentQuestionContents(int limit);

    @Select("SELECT content FROM chat_message WHERE role = 'user' AND create_time >= #{startDate} AND create_time <= #{endDate} ORDER BY create_time DESC LIMIT #{limit}")
    List<String> findQuestionContentsByDate(@Param("startDate") String startDate, @Param("endDate") String endDate, @Param("limit") int limit);

    @Select("SELECT m.id, m.content as question, m.create_time, m.citation, m.source_type, " +
            "u.username as studentName, u.real_name as studentRealName " +
            "FROM chat_message m " +
            "LEFT JOIN chat_session s ON m.session_id = s.id " +
            "LEFT JOIN sys_user u ON s.user_id = u.id AND (u.deleted = 0 OR u.deleted IS NULL) " +
            "WHERE m.role = 'user' " +
            "ORDER BY m.create_time DESC LIMIT #{limit}")
    List<Map<String, Object>> findRecentQuestions(int limit);

    @Select("SELECT m.id, m.content as question, m.create_time, m.citation, m.source_type, " +
            "u.username as studentName, u.real_name as studentRealName " +
            "FROM chat_message m " +
            "LEFT JOIN chat_session s ON m.session_id = s.id " +
            "LEFT JOIN sys_user u ON s.user_id = u.id AND (u.deleted = 0 OR u.deleted IS NULL) " +
            "WHERE m.role = 'user' AND m.create_time >= #{startDate} AND m.create_time <= #{endDate} " +
            "ORDER BY m.create_time DESC LIMIT #{limit}")
    List<Map<String, Object>> findRecentQuestionsByDate(@Param("startDate") String startDate, @Param("endDate") String endDate, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM chat_message WHERE role = 'assistant' AND citation IS NOT NULL AND citation != ''")
    int countCitedAnswers();

    @Select("SELECT COUNT(*) FROM chat_message WHERE role = 'assistant' AND citation IS NOT NULL AND citation != '' AND create_time >= #{startDate} AND create_time <= #{endDate}")
    int countCitedAnswersByDate(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("SELECT COUNT(*) FROM chat_message WHERE role = 'assistant'")
    int countTotalAnswers();

    @Select("SELECT COUNT(*) FROM chat_message WHERE role = 'assistant' AND create_time >= #{startDate} AND create_time <= #{endDate}")
    int countTotalAnswersByDate(@Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("SELECT m.content FROM chat_message m " +
            "LEFT JOIN chat_session s ON m.session_id = s.id " +
            "WHERE m.role = 'user' AND s.user_id = #{userId} " +
            "ORDER BY m.create_time DESC LIMIT #{limit}")
    List<String> findUserQuestionContents(@Param("userId") Long userId, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM chat_message m " +
            "LEFT JOIN chat_session s ON m.session_id = s.id " +
            "WHERE m.role = 'user' AND s.user_id = #{userId}")
    int countUserQuestions(@Param("userId") Long userId);

    @Select("SELECT COUNT(DISTINCT m.session_id) FROM chat_message m " +
            "LEFT JOIN chat_session s ON m.session_id = s.id " +
            "WHERE m.role = 'assistant' AND s.user_id = #{userId} AND m.citation IS NOT NULL AND m.citation != ''")
    int countUserCitedAnswers(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM chat_message m " +
            "LEFT JOIN chat_session s ON m.session_id = s.id " +
            "WHERE m.role = 'assistant' AND s.user_id = #{userId}")
    int countUserTotalAnswers(@Param("userId") Long userId);

    @Select("SELECT m.id, m.content as question, m.create_time, m.citation, m.source_type " +
            "FROM chat_message m " +
            "LEFT JOIN chat_session s ON m.session_id = s.id " +
            "WHERE m.role = 'user' AND s.user_id = #{userId} " +
            "ORDER BY m.create_time DESC LIMIT #{limit}")
    List<Map<String, Object>> findUserRecentQuestions(@Param("userId") Long userId, @Param("limit") int limit);

    @Select("SELECT s.user_id as userId, MAX(m.create_time) as lastQuestionTime " +
            "FROM chat_message m " +
            "LEFT JOIN chat_session s ON m.session_id = s.id " +
            "WHERE m.role = 'user' " +
            "GROUP BY s.user_id")
    List<Map<String, Object>> findAllLastQuestionTimes();

    // ===== 班级维度统计 =====
    // 班级列表（用于前端班级选择器）
    @Select("SELECT c.id as id, c.name as name, c.status as status, c.start_time as startTime, c.end_time as endTime, " +
            "COUNT(DISTINCT cs.student_id) as studentCount " +
            "FROM clazz c " +
            "LEFT JOIN class_student cs ON cs.class_id = c.id " +
            "WHERE c.deleted = 0 " +
            "GROUP BY c.id, c.name, c.status, c.start_time, c.end_time " +
            "ORDER BY c.status DESC, c.id DESC")
    List<Map<String, Object>> findClassList();

    // 按班级ID统计问答概览
    @Select("SELECT COUNT(DISTINCT m.id) as totalQuestions, " +
            "COUNT(DISTINCT s.user_id) as activeUsers " +
            "FROM class_student cs " +
            "JOIN sys_user u ON u.id = cs.student_id AND u.deleted = 0 " +
            "JOIN chat_session s ON s.user_id = u.id AND s.deleted = 0 " +
            "JOIN chat_message m ON m.session_id = s.id AND m.role = 'user' " +
            "WHERE cs.class_id = #{classId}")
    Map<String, Object> findClassOverview(@Param("classId") Long classId);

    @Select("SELECT COUNT(DISTINCT m.id) as totalQuestions, " +
            "COUNT(DISTINCT s.user_id) as activeUsers " +
            "FROM class_student cs " +
            "JOIN sys_user u ON u.id = cs.student_id AND u.deleted = 0 " +
            "JOIN chat_session s ON s.user_id = u.id AND s.deleted = 0 " +
            "JOIN chat_message m ON m.session_id = s.id AND m.role = 'user' AND m.create_time >= #{startDate} AND m.create_time <= #{endDate} " +
            "WHERE cs.class_id = #{classId}")
    Map<String, Object> findClassOverviewByDate(@Param("classId") Long classId, @Param("startDate") String startDate, @Param("endDate") String endDate);

    // 按班级ID统计引用
    @Select("SELECT COUNT(*) FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "JOIN class_student cs ON cs.student_id = s.user_id AND cs.class_id = #{classId} " +
            "WHERE m.role = 'assistant' AND m.citation IS NOT NULL AND m.citation != ''")
    int countClassCitedAnswers(@Param("classId") Long classId);

    @Select("SELECT COUNT(*) FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "JOIN class_student cs ON cs.student_id = s.user_id AND cs.class_id = #{classId} " +
            "WHERE m.role = 'assistant' AND m.citation IS NOT NULL AND m.citation != '' AND m.create_time >= #{startDate} AND m.create_time <= #{endDate}")
    int countClassCitedAnswersByDate(@Param("classId") Long classId, @Param("startDate") String startDate, @Param("endDate") String endDate);

    @Select("SELECT COUNT(*) FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "JOIN class_student cs ON cs.student_id = s.user_id AND cs.class_id = #{classId} " +
            "WHERE m.role = 'assistant'")
    int countClassTotalAnswers(@Param("classId") Long classId);

    @Select("SELECT COUNT(*) FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "JOIN class_student cs ON cs.student_id = s.user_id AND cs.class_id = #{classId} " +
            "WHERE m.role = 'assistant' AND m.create_time >= #{startDate} AND m.create_time <= #{endDate}")
    int countClassTotalAnswersByDate(@Param("classId") Long classId, @Param("startDate") String startDate, @Param("endDate") String endDate);

    // 按班级ID获取学生提问内容（用于关键词统计）
    @Select("SELECT m.content FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "JOIN class_student cs ON cs.student_id = s.user_id AND cs.class_id = #{classId} " +
            "WHERE m.role = 'user' " +
            "ORDER BY m.create_time DESC LIMIT #{limit}")
    List<String> findClassQuestionContents(@Param("classId") Long classId, @Param("limit") int limit);

    @Select("SELECT m.content FROM chat_message m " +
            "JOIN chat_session s ON m.session_id = s.id AND s.deleted = 0 " +
            "JOIN class_student cs ON cs.student_id = s.user_id AND cs.class_id = #{classId} " +
            "WHERE m.role = 'user' AND m.create_time >= #{startDate} AND m.create_time <= #{endDate} " +
            "ORDER BY m.create_time DESC LIMIT #{limit}")
    List<String> findClassQuestionContentsByDate(@Param("classId") Long classId, @Param("startDate") String startDate, @Param("endDate") String endDate, @Param("limit") int limit);
}
