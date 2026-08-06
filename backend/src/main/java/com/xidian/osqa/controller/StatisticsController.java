package com.xidian.osqa.controller;

import com.xidian.osqa.common.Result;
import com.xidian.osqa.service.StatisticsService;
import com.xidian.osqa.service.StudyTimeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final StudyTimeService studyTimeService;

    public StatisticsController(StatisticsService statisticsService, StudyTimeService studyTimeService) {
        this.statisticsService = statisticsService;
        this.studyTimeService = studyTimeService;
    }

    @GetMapping("/qa")
    public Result<?> getOverview(
            HttpServletRequest request,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Long teacherId = (Long) request.getAttribute("userId");
        Map<String, Object> overview = statisticsService.getOverview(teacherId, startDate, endDate);
        return Result.success(overview);
    }

    @GetMapping("/keywords")
    public Result<?> getHotKeywords(
            HttpServletRequest request,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "30") int limit) {
        Long teacherId = (Long) request.getAttribute("userId");
        List<Map<String, Object>> keywords = statisticsService.getHotKeywords(teacherId, startDate, endDate, limit);
        return Result.success(keywords);
    }

    @GetMapping("/recent")
    public Result<?> getRecentQuestions(
            HttpServletRequest request,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "20") int limit) {
        Long teacherId = (Long) request.getAttribute("userId");
        List<Map<String, Object>> recent = statisticsService.getRecentQuestions(teacherId, startDate, endDate, limit);
        return Result.success(recent);
    }

    @GetMapping("/user/{userId}/questions")
    public Result<?> getUserQuestions(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) Long classId,
            HttpServletRequest request) {
        Long teacherId = (Long) request.getAttribute("userId");
        List<Map<String, Object>> questions = statisticsService.getUserRecentQuestions(userId, teacherId, classId, limit);
        return Result.success(questions);
    }

    @GetMapping("/classes")
    public Result<?> getClassList(HttpServletRequest request) {
        Long teacherId = (Long) request.getAttribute("userId");
        List<Map<String, Object>> list = statisticsService.getClassList(teacherId);
        return Result.success(list);
    }

    @GetMapping("/classes/{classId}/overview")
    public Result<?> getClassOverview(
            @PathVariable Long classId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletRequest request) {
        Long teacherId = (Long) request.getAttribute("userId");
        if (denied(classId, teacherId)) return Result.error(403, "无权访问该班级数据");
        Map<String, Object> overview = statisticsService.getClassOverview(classId, startDate, endDate);
        return Result.success(overview);
    }

    @GetMapping("/classes/{classId}/keywords")
    public Result<?> getClassHotKeywords(
            @PathVariable Long classId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "30") int limit,
            HttpServletRequest request) {
        Long teacherId = (Long) request.getAttribute("userId");
        if (denied(classId, teacherId)) return Result.error(403, "无权访问该班级数据");
        List<Map<String, Object>> keywords = statisticsService.getClassHotKeywords(classId, startDate, endDate, limit);
        return Result.success(keywords);
    }

    // ===== 新增统计维度接口 =====

    @GetMapping("/trend")
    public Result<?> getQuestionTrend(
            HttpServletRequest request,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "daily") String granularity) {
        Long teacherId = (Long) request.getAttribute("userId");
        return Result.success(statisticsService.getQuestionTrend(teacherId, startDate, endDate, granularity));
    }

    @GetMapping("/classes/{classId}/trend")
    public Result<?> getClassQuestionTrend(
            @PathVariable Long classId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "daily") String granularity,
            HttpServletRequest request) {
        Long teacherId = (Long) request.getAttribute("userId");
        if (denied(classId, teacherId)) return Result.error(403, "无权访问该班级数据");
        return Result.success(statisticsService.getClassQuestionTrend(classId, startDate, endDate, granularity));
    }

    @GetMapping("/sessions")
    public Result<?> getSessionRounds(
            HttpServletRequest request,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "20") int limit) {
        Long teacherId = (Long) request.getAttribute("userId");
        return Result.success(statisticsService.getSessionRounds(teacherId, startDate, endDate, limit));
    }

    @GetMapping("/classes/{classId}/sessions")
    public Result<?> getClassSessionRounds(
            @PathVariable Long classId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest request) {
        Long teacherId = (Long) request.getAttribute("userId");
        if (denied(classId, teacherId)) return Result.error(403, "无权访问该班级数据");
        return Result.success(statisticsService.getClassSessionRounds(classId, startDate, endDate, limit));
    }

    @GetMapping("/sources")
    public Result<?> getSourceDistribution(
            HttpServletRequest request,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Long teacherId = (Long) request.getAttribute("userId");
        return Result.success(statisticsService.getSourceDistribution(teacherId, startDate, endDate));
    }

    @GetMapping("/classes/{classId}/sources")
    public Result<?> getClassSourceDistribution(
            @PathVariable Long classId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletRequest request) {
        Long teacherId = (Long) request.getAttribute("userId");
        if (denied(classId, teacherId)) return Result.error(403, "无权访问该班级数据");
        return Result.success(statisticsService.getClassSourceDistribution(classId, startDate, endDate));
    }

    @GetMapping("/active-days")
    public Result<?> getActiveDaysStats(
            HttpServletRequest request,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Long teacherId = (Long) request.getAttribute("userId");
        return Result.success(statisticsService.getActiveDaysStats(teacherId, startDate, endDate));
    }

    @GetMapping("/classes/{classId}/active-days")
    public Result<?> getClassActiveDaysStats(
            @PathVariable Long classId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletRequest request) {
        Long teacherId = (Long) request.getAttribute("userId");
        if (denied(classId, teacherId)) return Result.error(403, "无权访问该班级数据");
        return Result.success(statisticsService.getClassActiveDaysStats(classId, startDate, endDate));
    }

    // ===== 学习时长统计 =====

    @GetMapping("/study-time")
    public Result<?> getStudyTimeStats(
            HttpServletRequest request,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Long teacherId = (Long) request.getAttribute("userId");
        return Result.success(studyTimeService.getTeacherStudyTimeStats(teacherId, startDate, endDate));
    }

    @GetMapping("/classes/{classId}/study-time")
    public Result<?> getClassStudyTimeStats(
            @PathVariable Long classId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletRequest request) {
        Long teacherId = (Long) request.getAttribute("userId");
        if (denied(classId, teacherId)) return Result.error(403, "无权访问该班级数据");
        return Result.success(studyTimeService.getClassStudyTimeStats(classId, startDate, endDate));
    }

    // ===== 提问时段分布 =====

    @GetMapping("/hourly")
    public Result<?> getHourlyDistribution(
            HttpServletRequest request,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Long teacherId = (Long) request.getAttribute("userId");
        return Result.success(statisticsService.getHourlyDistribution(teacherId, startDate, endDate));
    }

    @GetMapping("/classes/{classId}/hourly")
    public Result<?> getClassHourlyDistribution(
            @PathVariable Long classId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletRequest request) {
        Long teacherId = (Long) request.getAttribute("userId");
        if (denied(classId, teacherId)) return Result.error(403, "无权访问该班级数据");
        return Result.success(statisticsService.getClassHourlyDistribution(classId, startDate, endDate));
    }

    // ===== 学生个体排行 =====

    @GetMapping("/student-ranking")
    public Result<?> getStudentRanking(
            HttpServletRequest request,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Long teacherId = (Long) request.getAttribute("userId");
        return Result.success(statisticsService.getStudentRanking(teacherId, startDate, endDate));
    }

    @GetMapping("/classes/{classId}/student-ranking")
    public Result<?> getClassStudentRanking(
            @PathVariable Long classId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            HttpServletRequest request) {
        Long teacherId = (Long) request.getAttribute("userId");
        if (denied(classId, teacherId)) return Result.error(403, "无权访问该班级数据");
        return Result.success(statisticsService.getClassStudentRanking(classId, startDate, endDate));
    }

    /** 校验当前教师是否有权访问该班级（越权防护） */
    private boolean denied(Long classId, Long teacherId) {
        return teacherId == null || !statisticsService.isClassOwnedByTeacher(classId, teacherId);
    }
}
