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
    public Result<?> getOverview() {
        Map<String, Object> overview = statisticsService.getOverview();
        return Result.success(overview);
    }

    @GetMapping("/keywords")
    public Result<?> getHotKeywords(@RequestParam(defaultValue = "30") int limit) {
        List<Map<String, Object>> keywords = statisticsService.getHotKeywords(limit);
        return Result.success(keywords);
    }

    @GetMapping("/trend")
    public Result<?> getQuestionTrend() {
        List<Map<String, Object>> trend = statisticsService.getQuestionTrend();
        return Result.success(trend);
    }

    @GetMapping("/recent")
    public Result<?> getRecentQuestions(@RequestParam(defaultValue = "20") int limit) {
        List<Map<String, Object>> recent = statisticsService.getRecentQuestions(limit);
        return Result.success(recent);
    }
}
