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
    public Result<?> getKnowledgeList(@RequestParam(required = false) Long kbId) {
        List<Knowledge> list = knowledgeService.getKnowledgeList(kbId);
        return Result.success(list);
    }

    @GetMapping("/status")
    public Result<?> getKnowledgeStatus(@RequestParam(required = false) Long kbId) {
        Map<String, Object> status = knowledgeService.getKnowledgeStatus(kbId);
        return Result.success(status);
    }

    @PostMapping("/upload")
    public Result<?> uploadKnowledge(@RequestParam("file") MultipartFile file,
                                     @RequestParam("kbId") Long kbId) {
        try {
            Knowledge knowledge = knowledgeService.uploadKnowledge(kbId, file);
            return Result.success(knowledge);
        } catch (Exception e) {
            return Result.error("上传失败：" + e.getMessage());
        }
    }

    @PostMapping("/import-text")
    public Result<?> importText(@RequestBody Map<String, Object> body) {
        try {
            String title = body.getOrDefault("title", "教材文本导入").toString();
            String content = body.get("content") == null ? null : body.get("content").toString();
            if (content == null || content.isBlank()) {
                return Result.error("内容不能为空");
            }
            Object kbIdObj = body.get("kbId");
            Long kbId = kbIdObj == null ? null : Long.valueOf(kbIdObj.toString());
            Knowledge knowledge = knowledgeService.importText(kbId, title, content);
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
