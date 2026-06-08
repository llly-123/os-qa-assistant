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
}
