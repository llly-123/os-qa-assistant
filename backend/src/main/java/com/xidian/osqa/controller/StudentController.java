package com.xidian.osqa.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xidian.osqa.common.Result;
import com.xidian.osqa.entity.User;
import com.xidian.osqa.service.StudentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/students")
public class StudentController {

    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    public Result<?> getStudentList(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String college,
            @RequestParam(required = false) Integer status) {
        Long teacherId = (Long) request.getAttribute("userId");
        Page<User> result = studentService.getStudentList(page, size, keyword, college, status, teacherId);
        return Result.success(result);
    }

    // 获取当前教师的所有学生（不分页，用于班级管理中勾选学生加入班级）
    @GetMapping("/all")
    public Result<?> getAllStudents(HttpServletRequest request) {
        Long teacherId = (Long) request.getAttribute("userId");
        return Result.success(studentService.getAllStudents(teacherId));
    }

    @PostMapping
    public Result<?> createStudent(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long teacherId = (Long) request.getAttribute("userId");
        String studentId = body.get("studentId");
        String name = body.get("name");
        String phone = body.get("phone");
        String college = body.get("college");
        String major = body.get("major");
        String grade = body.get("grade");
        return studentService.createStudent(studentId, name, phone, college, major, grade, teacherId);
    }

    @PostMapping("/import")
    public Result<?> batchImport(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        Long teacherId = (Long) request.getAttribute("userId");
        try {
            Map<String, Object> result = studentService.batchImport(file, teacherId);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("导入失败：" + e.getMessage());
        }
    }

    @PostMapping("/{id}/reset-password")
    public Result<?> resetPassword(HttpServletRequest request, @PathVariable Long id) {
        Long teacherId = (Long) request.getAttribute("userId");
        Map<String, Object> result = studentService.resetPassword(id, teacherId);
        if (result == null) {
            return Result.error("学生不存在或无权操作");
        }
        return Result.success(result);
    }

    @PutMapping("/{id}/status")
    public Result<?> toggleStatus(HttpServletRequest request, @PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Long teacherId = (Long) request.getAttribute("userId");
        Integer status = body.get("status");
        studentService.toggleStatus(id, status, teacherId);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<?> updateStudent(HttpServletRequest request, @PathVariable Long id, @RequestBody Map<String, String> body) {
        Long teacherId = (Long) request.getAttribute("userId");
        studentService.updateStudent(id, body, teacherId);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteStudent(HttpServletRequest request, @PathVariable Long id) {
        Long teacherId = (Long) request.getAttribute("userId");
        studentService.deleteStudent(id, teacherId);
        return Result.success();
    }

    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) throws Exception {
        studentService.downloadTemplate(response);
    }
}
