package com.xidian.osqa.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xidian.osqa.common.Result;
import com.xidian.osqa.entity.User;
import com.xidian.osqa.service.StudentService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
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
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        Page<User> result = studentService.getStudentList(page, size, keyword);
        return Result.success(result);
    }

    @PostMapping
    public Result<?> createStudent(@RequestBody Map<String, String> body) {
        String studentId = body.get("studentId");
        String name = body.get("name");
        String phone = body.get("phone");
        String college = body.get("college");
        String major = body.get("major");
        String grade = body.get("grade");
        return studentService.createStudent(studentId, name, phone, college, major, grade);
    }

    @PostMapping("/import")
    public Result<?> batchImport(@RequestParam("file") MultipartFile file) {
        try {
            Map<String, Object> result = studentService.batchImport(file);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("导入失败：" + e.getMessage());
        }
    }

    @PostMapping("/{id}/reset-password")
    public Result<?> resetPassword(@PathVariable Long id) {
        Map<String, Object> result = studentService.resetPassword(id);
        if (result == null) {
            return Result.error("学生不存在");
        }
        return Result.success(result);
    }

    @PutMapping("/{id}/status")
    public Result<?> toggleStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        studentService.toggleStatus(id, status);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<?> updateStudent(@PathVariable Long id, @RequestBody Map<String, String> body) {
        studentService.updateStudent(id, body);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return Result.success();
    }

    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) throws Exception {
        studentService.downloadTemplate(response);
    }
}
