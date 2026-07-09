package com.xidian.osqa.controller;

import com.xidian.osqa.common.Result;
import com.xidian.osqa.service.KnowledgeBaseService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/knowledge-bases")
public class KnowledgeBaseController {

    private final KnowledgeBaseService kbService;

    public KnowledgeBaseController(KnowledgeBaseService kbService) {
        this.kbService = kbService;
    }

    @GetMapping
    public Result<?> list(HttpServletRequest request) {
        Long teacherId = (Long) request.getAttribute("userId");
        return kbService.list(teacherId);
    }

    @PostMapping
    public Result<?> create(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long teacherId = (Long) request.getAttribute("userId");
        return kbService.create(teacherId, body.get("name"), body.get("description"));
    }

    @PutMapping("/{id}")
    public Result<?> update(HttpServletRequest request, @PathVariable Long id, @RequestBody Map<String, String> body) {
        Long teacherId = (Long) request.getAttribute("userId");
        return kbService.update(id, teacherId, body.get("name"), body.get("description"));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(HttpServletRequest request, @PathVariable Long id) {
        Long teacherId = (Long) request.getAttribute("userId");
        return kbService.delete(id, teacherId);
    }
}
