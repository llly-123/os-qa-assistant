package com.xidian.osqa.controller;

import com.xidian.osqa.common.Result;
import com.xidian.osqa.entity.Clazz;
import com.xidian.osqa.entity.VideoProgress;
import com.xidian.osqa.mapper.VideoProgressMapper;
import com.xidian.osqa.service.VideoService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class VideoController {

    private final VideoService videoService;
    private final VideoProgressMapper videoProgressMapper;
    private final com.xidian.osqa.service.ClazzService clazzService;

    @Value("${video.upload-dir:${user.dir}/uploads/videos}")
    private String uploadDir;

    @Value("${video.max-size:524288000}")
    private long maxVideoSize;

    public VideoController(VideoService videoService, VideoProgressMapper videoProgressMapper, com.xidian.osqa.service.ClazzService clazzService) {
        this.videoService = videoService;
        this.videoProgressMapper = videoProgressMapper;
        this.clazzService = clazzService;
    }

    // ========== 公共：获取章节列表 ==========

    @GetMapping("/courses/chapters")
    public Result<?> getChapters(@RequestParam(required = false) Long videoSetId) {
        return videoService.getChapters(videoSetId);
    }

    // ========== 教师端：章管理 ==========

    @PostMapping("/admin/chapters")
    public Result<?> addChapter(@RequestBody Map<String, Object> body) {
        Object vsId = body.get("videoSetId");
        Long videoSetId = vsId == null ? null : Long.valueOf(vsId.toString());
        return videoService.addChapter(videoSetId, (String) body.get("title"));
    }

    @PutMapping("/admin/chapters/{id}")
    public Result<?> updateChapter(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return videoService.updateChapter(id, body.get("title"));
    }

    @DeleteMapping("/admin/chapters/{id}")
    public Result<?> deleteChapter(@PathVariable Long id) {
        return videoService.deleteChapter(id);
    }

    @PutMapping("/admin/chapters/{id}/move")
    public Result<?> moveChapter(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return videoService.moveChapter(id, body.get("direction"));
    }

    // ========== 教师端：节管理 ==========

    @PostMapping("/admin/chapters/{chapterId}/sections")
    public Result<?> addSection(@PathVariable Long chapterId, @RequestBody Map<String, String> body) {
        return videoService.addSection(chapterId, body.get("title"));
    }

    @PutMapping("/admin/sections/{id}")
    public Result<?> updateSection(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return videoService.updateSection(id, body.get("title"));
    }

    @DeleteMapping("/admin/sections/{id}")
    public Result<?> deleteSection(@PathVariable Long id) {
        return videoService.deleteSection(id);
    }

    @PutMapping("/admin/sections/{id}/move")
    public Result<?> moveSection(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return videoService.moveSection(id, body.get("direction"));
    }

    // ========== 教师端：视频上传 ==========

    @PostMapping("/admin/sections/{sectionId}/upload-video")
    public Result<?> uploadVideo(@PathVariable Long sectionId, @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error(400, "请选择视频文件");
        }

        if (file.getSize() > maxVideoSize) {
            return Result.error(400, "视频文件不能超过500MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            String originalName = file.getOriginalFilename();
            if (originalName == null || !originalName.matches(".*\\.(mp4|webm|avi|mov|mkv)$")) {
                return Result.error(400, "仅支持视频文件格式（mp4, webm, avi, mov, mkv）");
            }
        }

        try {
            Path dirPath = Paths.get(uploadDir, String.valueOf(sectionId));
            Files.createDirectories(dirPath);

            String originalName = file.getOriginalFilename();
            String ext = originalName != null && originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf("."))
                    : ".mp4";
            String fileName = sectionId + ext;

            Path filePath = dirPath.resolve(fileName);
            file.transferTo(filePath.toFile());

            String videoUrl = "/api/videos/" + sectionId + "/" + fileName;
            Long videoSize = file.getSize();

            videoService.updateSectionVideo(sectionId, videoUrl, videoSize, 0);

            Map<String, Object> result = new HashMap<>();
            result.put("videoUrl", videoUrl);
            result.put("videoSize", videoSize);
            return Result.success(result);

        } catch (IOException e) {
            return Result.error(500, "视频上传失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/admin/sections/{sectionId}/video")
    public Result<?> deleteVideo(@PathVariable Long sectionId) {
        try {
            Path dirPath = Paths.get(uploadDir, String.valueOf(sectionId));
            File dir = dirPath.toFile();
            if (dir.exists()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        f.delete();
                    }
                }
                dir.delete();
            }
        } catch (Exception e) {
            // ignore cleanup errors
        }
        return videoService.deleteSectionVideo(sectionId);
    }

    // ========== 视频文件访问 ==========

    @GetMapping("/videos/{sectionId}/{fileName}")
    public ResponseEntity<Resource> getVideo(@PathVariable Long sectionId, @PathVariable String fileName) {
        try {
            Path filePath = Paths.get(uploadDir, String.valueOf(sectionId), fileName);
            File file = filePath.toFile();
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            FileSystemResource resource = new FileSystemResource(file);
            String mimeType = Files.probeContentType(filePath);
            if (mimeType == null) mimeType = "video/mp4";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mimeType))
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(file.length()))
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ========== 学生端：章节（按班级）==========

    @GetMapping("/students/classes/{classId}/chapters")
    public Result<?> getClassChapters(HttpServletRequest request, @PathVariable Long classId) {
        Long userId = (Long) request.getAttribute("userId");
        Clazz clazz = clazzService.getClazzForStudent(classId, userId);
        if (clazz == null) {
            return Result.error(403, "未加入该班级或班级已失效");
        }
        return videoService.getChapters(clazz.getVideoSetId());
    }

    // ========== 学生端：视频进度 ==========

    @PostMapping("/students/video-progress")
    public Result<?> saveProgress(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return Result.error(401, "未登录");
            }

            // 按班级校验成员关系
            Object classIdObj = body.get("classId");
            Clazz clazz = null;
            if (classIdObj != null) {
                Long classId = Long.valueOf(classIdObj.toString());
                clazz = clazzService.getClazzForStudent(classId, userId);
            }
            if (clazz == null) {
                clazz = clazzService.getStudentActiveClass(userId);
            }
            if (clazz == null) {
                return Result.error(403, "请先进入班级");
            }

            Object sectionIdObj = body.get("sectionId");
            Object currentTimeObj = body.get("currentTime");
            Object completedObj = body.get("completed");

            if (sectionIdObj == null) {
                return Result.error(400, "sectionId不能为空");
            }

            Long sectionId = Long.valueOf(sectionIdObj.toString());
            Double playTime = currentTimeObj != null ? Double.valueOf(currentTimeObj.toString()) : 0.0;
            boolean completed = false;
            if (completedObj instanceof Boolean) {
                completed = (Boolean) completedObj;
            } else if (completedObj != null) {
                completed = "true".equalsIgnoreCase(completedObj.toString()) || "1".equals(completedObj.toString());
            }

            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<VideoProgress> wrapper =
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            wrapper.eq(VideoProgress::getUserId, userId);
            wrapper.eq(VideoProgress::getSectionId, sectionId);

            VideoProgress progress = videoProgressMapper.selectOne(wrapper);
            if (progress == null) {
                progress = new VideoProgress();
                progress.setUserId(userId);
                progress.setSectionId(sectionId);
                progress.setPlayTime(playTime);
                progress.setCompleted(completed ? 1 : 0);
                progress.setCreateTime(LocalDateTime.now());
                progress.setUpdateTime(LocalDateTime.now());
                videoProgressMapper.insert(progress);
            } else {
                progress.setPlayTime(playTime);
                if (completed) {
                    progress.setCompleted(1);
                }
                progress.setUpdateTime(LocalDateTime.now());
                videoProgressMapper.updateById(progress);
            }

            return Result.success();
        } catch (Exception e) {
            return Result.error(500, "保存进度失败: " + e.getMessage());
        }
    }

    @GetMapping("/students/video-progress")
    public Result<?> getProgress(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return Result.error(401, "未登录");
            }
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<VideoProgress> wrapper =
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            wrapper.eq(VideoProgress::getUserId, userId);
            List<VideoProgress> progressList = videoProgressMapper.selectList(wrapper);
            return Result.success(progressList);
        } catch (Exception e) {
            return Result.error(500, "获取进度失败: " + e.getMessage());
        }
    }
}
