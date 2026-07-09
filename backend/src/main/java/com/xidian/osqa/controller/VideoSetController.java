package com.xidian.osqa.controller;

import com.xidian.osqa.common.Result;
import com.xidian.osqa.service.VideoSetService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/video-sets")
public class VideoSetController {

    private final VideoSetService videoSetService;

    public VideoSetController(VideoSetService videoSetService) {
        this.videoSetService = videoSetService;
    }

    @GetMapping
    public Result<?> list(HttpServletRequest request) {
        Long teacherId = (Long) request.getAttribute("userId");
        return videoSetService.list(teacherId);
    }

    @PostMapping
    public Result<?> create(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long teacherId = (Long) request.getAttribute("userId");
        return videoSetService.create(teacherId, body.get("name"), body.get("description"));
    }

    @PutMapping("/{id}")
    public Result<?> update(HttpServletRequest request, @PathVariable Long id, @RequestBody Map<String, String> body) {
        Long teacherId = (Long) request.getAttribute("userId");
        return videoSetService.update(id, teacherId, body.get("name"), body.get("description"));
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(HttpServletRequest request, @PathVariable Long id) {
        Long teacherId = (Long) request.getAttribute("userId");
        return videoSetService.delete(id, teacherId);
    }

    /** 取视频集下的章/节 */
    @GetMapping("/{setId}/chapters")
    public Result<?> chapters(@PathVariable Long setId) {
        return videoSetService.chaptersOf(setId);
    }
}
