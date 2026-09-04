package com.xidian.osqa.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xidian.osqa.common.Result;
import com.xidian.osqa.entity.User;
import com.xidian.osqa.service.TeacherService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/teachers")
public class TeacherController {

    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @GetMapping
    public Result<?> getTeacherList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer auditStatus) {
        Page<User> result = teacherService.getTeacherList(page, size, keyword, auditStatus);
        return Result.success(result);
    }

    @PutMapping("/{id}/audit")
    public Result<?> auditTeacher(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        return teacherService.auditTeacher(id, body.get("auditStatus"));
    }

    @PutMapping("/{id}/status")
    public Result<?> toggleStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        return teacherService.toggleStatus(id, body.get("status"));
    }

    @PostMapping("/{id}/reset-password")
    public Result<?> resetPassword(@PathVariable Long id) {
        return teacherService.resetPassword(id);
    }

    /** 设置教师的 API 体验时间段 */
    @PutMapping("/{id}/trial")
    public Result<?> setTrialPeriod(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return teacherService.setTrialPeriod(id, body.get("startTime"), body.get("endTime"));
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteTeacher(@PathVariable Long id) {
        return teacherService.deleteTeacher(id);
    }
}
