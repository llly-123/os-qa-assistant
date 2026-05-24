package com.xidian.osqa.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xidian.osqa.common.Result;
import com.xidian.osqa.entity.User;
import com.xidian.osqa.service.StudentService;
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
        String email = body.get("email");
        User user = studentService.createStudent(studentId, name, email);
        return Result.success(user);
    }

    @PostMapping("/import")
    public Result<?> batchImport(@RequestParam("file") MultipartFile file) {
        return Result.success("导入功能需要EasyExcel解析");
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

    @DeleteMapping("/{id}")
    public Result<?> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return Result.success();
    }
}
