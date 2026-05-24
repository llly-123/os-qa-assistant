package com.xidian.osqa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xidian.osqa.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    @Select("SELECT COUNT(*) as total_questions FROM chat_message WHERE role = 'user'")
    int countTotalQuestions();

    @Select("SELECT COUNT(DISTINCT session_id) as active_sessions FROM chat_message WHERE role = 'user'")
    int countActiveSessions();

    @Select("SELECT content FROM chat_message WHERE role = 'user' ORDER BY create_time DESC LIMIT #{limit}")
    List<String> findRecentQuestionContents(int limit);
}
