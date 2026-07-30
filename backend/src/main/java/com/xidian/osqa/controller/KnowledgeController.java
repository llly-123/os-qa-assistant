package com.xidian.osqa.controller;

import com.xidian.osqa.common.Result;
import com.xidian.osqa.entity.Knowledge;
import com.xidian.osqa.service.KnowledgeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @GetMapping
    public Result<?> getKnowledgeList(HttpServletRequest request, @RequestParam(required = false) Long kbId) {
        Long teacherId = (Long) request.getAttribute("userId");
        return knowledgeService.getKnowledgeList(teacherId, kbId);
    }

    @GetMapping("/status")
    public Result<?> getKnowledgeStatus(HttpServletRequest request, @RequestParam(required = false) Long kbId) {
        Long teacherId = (Long) request.getAttribute("userId");
        return knowledgeService.getKnowledgeStatus(teacherId, kbId);
    }

    @PostMapping("/upload")
    public Result<?> uploadKnowledge(HttpServletRequest request,
                                     @RequestParam("file") MultipartFile file,
                                     @RequestParam("kbId") Long kbId) {
        Long teacherId = (Long) request.getAttribute("userId");
        try {
            Knowledge knowledge = knowledgeService.uploadKnowledge(teacherId, kbId, file);
            return Result.success(knowledge);
        } catch (Exception e) {
            return Result.error("上传失败：" + e.getMessage());
        }
    }

    @PostMapping("/import-text")
    public Result<?> importText(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        Long teacherId = (Long) request.getAttribute("userId");
        try {
            String title = body.getOrDefault("title", "教材文本导入").toString();
            String content = body.get("content") == null ? null : body.get("content").toString();
            if (content == null || content.isBlank()) {
                return Result.error("内容不能为空");
            }
            Object kbIdObj = body.get("kbId");
            Long kbId = kbIdObj == null ? null : Long.valueOf(kbIdObj.toString());
            Knowledge knowledge = knowledgeService.importText(teacherId, kbId, title, content);
            return Result.success(knowledge);
        } catch (Exception e) {
            return Result.error("导入失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteKnowledge(HttpServletRequest request, @PathVariable Long id) {
        Long teacherId = (Long) request.getAttribute("userId");
        return knowledgeService.deleteKnowledge(id, teacherId);
    }

    @PostMapping("/rebuild")
    public Result<?> rebuildIndex() {
        knowledgeService.rebuildIndex();
        return Result.success();
    }
}
