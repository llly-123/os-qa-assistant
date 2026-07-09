package com.xidian.osqa.controller;

import com.xidian.osqa.common.Result;
import com.xidian.osqa.service.ClazzService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ClazzController {

    private final ClazzService clazzService;

    public ClazzController(ClazzService clazzService) {
        this.clazzService = clazzService;
    }

    // ========== 教师端 ==========

    @GetMapping("/admin/classes")
    public Result<?> getClasses(HttpServletRequest request) {
        Long teacherId = (Long) request.getAttribute("userId");
        return clazzService.getClasses(teacherId);
    }

    @PostMapping("/admin/classes")
    public Result<?> createClass(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long teacherId = (Long) request.getAttribute("userId");
        String name = body.get("name") == null ? null : body.get("name").toString();
        String startTime = body.get("startTime") == null ? null : body.get("startTime").toString();
        String endTime = body.get("endTime") == null ? null : body.get("endTime").toString();
        Long videoSetId = body.get("videoSetId") == null ? null : Long.valueOf(body.get("videoSetId").toString());
        Long kbId = body.get("kbId") == null ? null : Long.valueOf(body.get("kbId").toString());
        return clazzService.createClass(teacherId, name, startTime, endTime, videoSetId, kbId);
    }

    @DeleteMapping("/admin/classes/{classId}")
    public Result<?> deleteClass(HttpServletRequest request, @PathVariable Long classId) {
        Long teacherId = (Long) request.getAttribute("userId");
        return clazzService.deleteClass(classId, teacherId);
    }

    @PostMapping("/admin/classes/{classId}/dissolve")
    public Result<?> dissolveClass(HttpServletRequest request, @PathVariable Long classId) {
        Long teacherId = (Long) request.getAttribute("userId");
        return clazzService.dissolveClass(classId, teacherId);
    }

    // 为已存在班级挂载/修改/取消挂载视频集与知识库
    @PutMapping("/admin/classes/{classId}/resources")
    public Result<?> updateClassResources(HttpServletRequest request, @PathVariable Long classId, @RequestBody Map<String, Object> body) {
        Long teacherId = (Long) request.getAttribute("userId");
        Long videoSetId = body.get("videoSetId") == null ? null : Long.valueOf(body.get("videoSetId").toString());
        Long kbId = body.get("kbId") == null ? null : Long.valueOf(body.get("kbId").toString());
        return clazzService.updateClassResources(classId, teacherId, videoSetId, kbId);
    }

    @GetMapping("/admin/classes/{classId}/students")
    public Result<?> getClassStudents(@PathVariable Long classId) {
        return clazzService.getClassStudents(classId);
    }

    @PostMapping("/admin/classes/{classId}/students")
    public Result<?> addStudent(@PathVariable Long classId, @RequestBody Map<String, Object> body) {
        Long studentId = Long.valueOf(body.get("studentId").toString());
        return clazzService.addStudent(classId, studentId);
    }

    @PostMapping("/admin/classes/{classId}/students/batch")
    public Result<?> addStudentsByUsernames(@PathVariable Long classId, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> usernames = (List<String>) body.get("usernames");
        return clazzService.addStudentsByUsernames(classId, usernames);
    }

    @DeleteMapping("/admin/classes/{classId}/students/{studentId}")
    public Result<?> removeStudent(@PathVariable Long classId, @PathVariable Long studentId) {
        return clazzService.removeStudent(classId, studentId);
    }

    @PostMapping("/admin/classes/{classId}/students/create")
    public Result<?> createStudentInClass(@PathVariable Long classId, @RequestBody Map<String, String> body) {
        return clazzService.createStudentInClass(classId, body);
    }

    @PostMapping("/admin/classes/{classId}/students/import")
    public Result<?> importStudentsInClass(@PathVariable Long classId, @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            return clazzService.importStudentsInClass(classId, file);
        } catch (Exception e) {
            return Result.error("导入失败：" + e.getMessage());
        }
    }

    @GetMapping("/admin/classes/{classId}/students/template")
    public void downloadTemplate(jakarta.servlet.http.HttpServletResponse response) throws Exception {
        clazzService.downloadTemplate(response);
    }

    // ========== 学生端 ==========

    @GetMapping("/students/my-class")
    public Result<?> getMyClass(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        var clazz = clazzService.getStudentActiveClass(userId);
        if (clazz == null) {
            return Result.success(null);
        }
        return Result.success(clazz);
    }

    @GetMapping("/students/my-classes")
    public Result<?> getMyClasses(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(clazzService.getStudentClasses(userId));
    }
}
