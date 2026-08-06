package com.xidian.osqa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xidian.osqa.entity.StudyTime;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface StudyTimeMapper extends BaseMapper<StudyTime> {

    // 学习时长统计（教师维度）：统计该教师所有班级学生的累计学习时长
    @Select("<script>" +
            "SELECT st.user_id as userId, u.username as username, u.real_name as realName, " +
            "c.id as classId, c.name as className, " +
            "SUM(st.total_seconds) as totalSeconds, MAX(st.update_time) as lastStudy " +
            "FROM study_time st " +
            "JOIN clazz c ON c.id = st.class_id AND c.deleted = 0 AND c.teacher_id = #{teacherId} " +
            "LEFT JOIN sys_user u ON u.id = st.user_id AND (u.deleted = 0 OR u.deleted IS NULL) " +
            "<where>" +
            "<if test='startDate != null'>AND st.study_date &gt;= #{startDate}</if>" +
            "<if test='endDate != null'>AND st.study_date &lt;= #{endDate}</if>" +
            "</where>" +
            "GROUP BY st.user_id, u.username, u.real_name, c.id, c.name " +
            "ORDER BY totalSeconds DESC" +
            "</script>")
    List<Map<String, Object>> findTeacherStudyTimeStats(@Param("teacherId") Long teacherId,
                                                        @Param("startDate") String startDate,
                                                        @Param("endDate") String endDate);

    // 学习时长统计（班级维度）：统计指定班级学生的累计学习时长
    @Select("<script>" +
            "SELECT st.user_id as userId, u.username as username, u.real_name as realName, " +
            "SUM(st.total_seconds) as totalSeconds, MAX(st.update_time) as lastStudy " +
            "FROM study_time st " +
            "LEFT JOIN sys_user u ON u.id = st.user_id AND (u.deleted = 0 OR u.deleted IS NULL) " +
            "<where>" +
            "st.class_id = #{classId} " +
            "<if test='startDate != null'>AND st.study_date &gt;= #{startDate}</if>" +
            "<if test='endDate != null'>AND st.study_date &lt;= #{endDate}</if>" +
            "</where>" +
            "GROUP BY st.user_id, u.username, u.real_name " +
            "ORDER BY totalSeconds DESC" +
            "</script>")
    List<Map<String, Object>> findClassStudyTimeStats(@Param("classId") Long classId,
                                                      @Param("startDate") String startDate,
                                                      @Param("endDate") String endDate);

    // 学习时长汇总（教师维度）
    @Select("<script>" +
            "SELECT COALESCE(SUM(st.total_seconds), 0) FROM study_time st " +
            "JOIN clazz c ON c.id = st.class_id AND c.deleted = 0 AND c.teacher_id = #{teacherId} " +
            "<where>" +
            "<if test='startDate != null'>AND st.study_date &gt;= #{startDate}</if>" +
            "<if test='endDate != null'>AND st.study_date &lt;= #{endDate}</if>" +
            "</where>" +
            "</script>")
    Integer sumTeacherStudySeconds(@Param("teacherId") Long teacherId,
                                   @Param("startDate") String startDate,
                                   @Param("endDate") String endDate);

    // 学习时长汇总（班级维度）
    @Select("<script>" +
            "SELECT COALESCE(SUM(st.total_seconds), 0) FROM study_time st " +
            "<where>" +
            "st.class_id = #{classId} " +
            "<if test='startDate != null'>AND st.study_date &gt;= #{startDate}</if>" +
            "<if test='endDate != null'>AND st.study_date &lt;= #{endDate}</if>" +
            "</where>" +
            "</script>")
    Integer sumClassStudySeconds(@Param("classId") Long classId,
                                 @Param("startDate") String startDate,
                                 @Param("endDate") String endDate);

    // 学习时长汇总（学生个人维度，可按班级过滤）
    @Select("<script>" +
            "SELECT COALESCE(SUM(st.total_seconds), 0) FROM study_time st " +
            "<where>" +
            "st.user_id = #{userId} " +
            "<if test='classId != null'>AND st.class_id = #{classId}</if>" +
            "<if test='startDate != null'>AND st.study_date &gt;= #{startDate}</if>" +
            "<if test='endDate != null'>AND st.study_date &lt;= #{endDate}</if>" +
            "</where>" +
            "</script>")
    Integer sumUserStudySeconds(@Param("userId") Long userId,
                                @Param("classId") Long classId,
                                @Param("startDate") String startDate,
                                @Param("endDate") String endDate);

    // ===== 学习时长趋势（教师/班级/学生维度，日期为 yyyy-MM-dd）=====

    @Select("SELECT FORMATDATETIME(st.study_date, 'yyyy-MM-dd') as date, SUM(st.total_seconds) as studySeconds " +
            "FROM study_time st " +
            "JOIN clazz c ON c.id = st.class_id AND c.deleted = 0 AND c.teacher_id = #{teacherId} " +
            "WHERE st.study_date >= #{startDate} AND st.study_date <= #{endDate} " +
            "GROUP BY FORMATDATETIME(st.study_date, 'yyyy-MM-dd') " +
            "ORDER BY date")
    List<Map<String, Object>> findTeacherDailyStudyTimeTrend(@Param("teacherId") Long teacherId,
                                                             @Param("startDate") String startDate,
                                                             @Param("endDate") String endDate);

    @Select("SELECT FORMATDATETIME(MIN(st.study_date), 'yyyy-MM-dd') as week, SUM(st.total_seconds) as studySeconds " +
            "FROM study_time st " +
            "JOIN clazz c ON c.id = st.class_id AND c.deleted = 0 AND c.teacher_id = #{teacherId} " +
            "WHERE st.study_date >= #{startDate} AND st.study_date <= #{endDate} " +
            "GROUP BY YEAR(st.study_date), WEEK(st.study_date) " +
            "ORDER BY week")
    List<Map<String, Object>> findTeacherWeeklyStudyTimeTrend(@Param("teacherId") Long teacherId,
                                                              @Param("startDate") String startDate,
                                                              @Param("endDate") String endDate);

    @Select("SELECT FORMATDATETIME(st.study_date, 'yyyy-MM-dd') as date, SUM(st.total_seconds) as studySeconds " +
            "FROM study_time st " +
            "WHERE st.class_id = #{classId} AND st.study_date >= #{startDate} AND st.study_date <= #{endDate} " +
            "GROUP BY FORMATDATETIME(st.study_date, 'yyyy-MM-dd') " +
            "ORDER BY date")
    List<Map<String, Object>> findClassDailyStudyTimeTrend(@Param("classId") Long classId,
                                                           @Param("startDate") String startDate,
                                                           @Param("endDate") String endDate);

    @Select("SELECT FORMATDATETIME(MIN(st.study_date), 'yyyy-MM-dd') as week, SUM(st.total_seconds) as studySeconds " +
            "FROM study_time st " +
            "WHERE st.class_id = #{classId} AND st.study_date >= #{startDate} AND st.study_date <= #{endDate} " +
            "GROUP BY YEAR(st.study_date), WEEK(st.study_date) " +
            "ORDER BY week")
    List<Map<String, Object>> findClassWeeklyStudyTimeTrend(@Param("classId") Long classId,
                                                            @Param("startDate") String startDate,
                                                            @Param("endDate") String endDate);

    @Select("<script>" +
            "SELECT FORMATDATETIME(st.study_date, 'yyyy-MM-dd') as date, SUM(st.total_seconds) as studySeconds " +
            "FROM study_time st " +
            "<where>" +
            "st.user_id = #{userId} " +
            "<if test='classId != null'>AND st.class_id = #{classId}</if>" +
            "AND st.study_date &gt;= #{startDate} AND st.study_date &lt;= #{endDate}" +
            "</where> " +
            "GROUP BY FORMATDATETIME(st.study_date, 'yyyy-MM-dd') " +
            "ORDER BY date" +
            "</script>")
    List<Map<String, Object>> findUserDailyStudyTimeTrend(@Param("userId") Long userId,
                                                          @Param("classId") Long classId,
                                                          @Param("startDate") String startDate,
                                                          @Param("endDate") String endDate);

    @Select("<script>" +
            "SELECT FORMATDATETIME(MIN(st.study_date), 'yyyy-MM-dd') as week, SUM(st.total_seconds) as studySeconds " +
            "FROM study_time st " +
            "<where>" +
            "st.user_id = #{userId} " +
            "<if test='classId != null'>AND st.class_id = #{classId}</if>" +
            "AND st.study_date &gt;= #{startDate} AND st.study_date &lt;= #{endDate}" +
            "</where> " +
            "GROUP BY YEAR(st.study_date), WEEK(st.study_date) " +
            "ORDER BY week" +
            "</script>")
    List<Map<String, Object>> findUserWeeklyStudyTimeTrend(@Param("userId") Long userId,
                                                           @Param("classId") Long classId,
                                                           @Param("startDate") String startDate,
                                                           @Param("endDate") String endDate);

    // 学生每日学习时长（用于活跃天数热力图叠加学习时长标志，可按班级过滤）
    @Select("<script>" +
            "SELECT FORMATDATETIME(st.study_date, 'yyyy-MM-dd') as date, SUM(st.total_seconds) as studySeconds " +
            "FROM study_time st " +
            "<where>" +
            "st.user_id = #{userId} " +
            "<if test='classId != null'>AND st.class_id = #{classId}</if>" +
            "AND st.study_date &gt;= #{startDate} AND st.study_date &lt;= #{endDate}" +
            "</where> " +
            "GROUP BY FORMATDATETIME(st.study_date, 'yyyy-MM-dd') " +
            "ORDER BY date" +
            "</script>")
    List<Map<String, Object>> findUserDailyStudySeconds(@Param("userId") Long userId,
                                                        @Param("classId") Long classId,
                                                        @Param("startDate") String startDate,
                                                        @Param("endDate") String endDate);
}
