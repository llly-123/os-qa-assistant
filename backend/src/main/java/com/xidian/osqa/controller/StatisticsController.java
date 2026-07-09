package com.xidian.osqa.controller;

import com.xidian.osqa.common.Result;
import com.xidian.osqa.service.StatisticsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
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
            @RequestParam(required = false) String endDate) {
        Map<String, Object> overview = statisticsService.getClassOverview(classId, startDate, endDate);
        return Result.success(overview);
    }

    @GetMapping("/classes/{classId}/keywords")
    public Result<?> getClassHotKeywords(
            @PathVariable Long classId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "30") int limit) {
        List<Map<String, Object>> keywords = statisticsService.getClassHotKeywords(classId, startDate, endDate, limit);
        return Result.success(keywords);
    }
}
