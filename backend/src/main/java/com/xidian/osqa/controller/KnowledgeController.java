package com.xidian.osqa.controller;

import com.xidian.osqa.common.Result;
import com.xidian.osqa.entity.Knowledge;
import com.xidian.osqa.service.KnowledgeService;
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
    public Result<?> getKnowledgeList() {
        List<Knowledge> list = knowledgeService.getKnowledgeList();
        return Result.success(list);
    }

    @GetMapping("/status")
    public Result<?> getKnowledgeStatus() {
        Map<String, Object> status = knowledgeService.getKnowledgeStatus();
        return Result.success(status);
    }

    @PostMapping("/upload")
    public Result<?> uploadKnowledge(@RequestParam("file") MultipartFile file) {
        try {
            Knowledge knowledge = knowledgeService.uploadKnowledge(file);
            return Result.success(knowledge);
        } catch (Exception e) {
            return Result.error("上传失败：" + e.getMessage());
        }
    }

    @PostMapping("/import-text")
    public Result<?> importText(@RequestBody Map<String, String> body) {
        try {
            String title = body.getOrDefault("title", "教材文本导入");
            String content = body.get("content");
            if (content == null || content.isBlank()) {
                return Result.error("内容不能为空");
            }
            Knowledge knowledge = knowledgeService.importText(title, content);
            return Result.success(knowledge);
        } catch (Exception e) {
            return Result.error("导入失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteKnowledge(@PathVariable Long id) {
        knowledgeService.deleteKnowledge(id);
        return Result.success();
    }

    @PostMapping("/rebuild")
    public Result<?> rebuildIndex() {
        knowledgeService.rebuildIndex();
        return Result.success();
    }
}
