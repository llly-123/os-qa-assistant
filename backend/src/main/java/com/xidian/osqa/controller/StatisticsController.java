package com.xidian.osqa.controller;

import com.xidian.osqa.common.Result;
import com.xidian.osqa.service.StatisticsService;
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
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Map<String, Object> overview = statisticsService.getOverview(startDate, endDate);
        return Result.success(overview);
    }

    @GetMapping("/keywords")
    public Result<?> getHotKeywords(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "30") int limit) {
        List<Map<String, Object>> keywords = statisticsService.getHotKeywords(startDate, endDate, limit);
        return Result.success(keywords);
    }

    @GetMapping("/recent")
    public Result<?> getRecentQuestions(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "20") int limit) {
        List<Map<String, Object>> recent = statisticsService.getRecentQuestions(startDate, endDate, limit);
        return Result.success(recent);
    }

    @GetMapping("/user/{userId}/questions")
    public Result<?> getUserQuestions(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "20") int limit) {
        List<Map<String, Object>> questions = statisticsService.getUserRecentQuestions(userId, limit);
        return Result.success(questions);
    }
}
