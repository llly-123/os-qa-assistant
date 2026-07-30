package com.xidian.osqa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xidian.osqa.common.Result;
import com.xidian.osqa.entity.Knowledge;
import com.xidian.osqa.entity.KnowledgeBase;
import com.xidian.osqa.mapper.KnowledgeBaseMapper;
import com.xidian.osqa.mapper.KnowledgeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);

    private final KnowledgeMapper knowledgeMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeEmbeddingService embeddingService;

    @Value("${knowledge.upload-dir}")
    private String uploadDir;

    public KnowledgeService(KnowledgeMapper knowledgeMapper, KnowledgeBaseMapper knowledgeBaseMapper, KnowledgeEmbeddingService embeddingService) {
        this.knowledgeMapper = knowledgeMapper;
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.embeddingService = embeddingService;
    }

    /** 校验知识库归属，返回 kbId（null 时返回 null） */
    private Long checkKbOwnership(Long teacherId, Long kbId) {
        if (kbId == null) return null;
        KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
        if (kb == null || !kb.getTeacherId().equals(teacherId)) {
            throw new SecurityException("无权操作此知识库");
        }
        return kbId;
    }

    public Result<?> getKnowledgeList(Long teacherId, Long kbId) {
        Long verifiedKbId = kbId != null ? checkKbOwnership(teacherId, kbId) : null;
        LambdaQueryWrapper<Knowledge> wrapper = new LambdaQueryWrapper<>();
        if (verifiedKbId != null) {
            wrapper.eq(Knowledge::getKbId, verifiedKbId);
        } else {
            // 不传 kbId 时只返回该教师所有知识库下的文档
            LambdaQueryWrapper<KnowledgeBase> kbWrapper = new LambdaQueryWrapper<>();
            kbWrapper.eq(KnowledgeBase::getTeacherId, teacherId);
            List<KnowledgeBase> kbs = knowledgeBaseMapper.selectList(kbWrapper);
            if (kbs.isEmpty()) return Result.success(List.of());
            wrapper.in(Knowledge::getKbId, kbs.stream().map(KnowledgeBase::getId).collect(java.util.stream.Collectors.toList()));
        }
        wrapper.orderByDesc(Knowledge::getCreateTime);
        return Result.success(knowledgeMapper.selectList(wrapper));
    }

    public Result<?> getKnowledgeStatus(Long teacherId, Long kbId) {
        Long verifiedKbId = kbId != null ? checkKbOwnership(teacherId, kbId) : null;
        Map<String, Object> status = new HashMap<>();
        try {
            LambdaQueryWrapper<Knowledge> w = new LambdaQueryWrapper<>();
            if (verifiedKbId != null) {
                w.eq(Knowledge::getKbId, verifiedKbId);
            } else {
                LambdaQueryWrapper<KnowledgeBase> kbWrapper = new LambdaQueryWrapper<>();
                kbWrapper.eq(KnowledgeBase::getTeacherId, teacherId);
                List<KnowledgeBase> kbs = knowledgeBaseMapper.selectList(kbWrapper);
                if (kbs.isEmpty()) {
                    status.put("documentCount", 0);
                    status.put("chunkCount", 0);
                    status.put("embeddingDimension", 1024);
                    status.put("lastUpdate", null);
                    return Result.success(status);
                }
                w.in(Knowledge::getKbId, kbs.stream().map(KnowledgeBase::getId).collect(java.util.stream.Collectors.toList()));
            }
            List<Knowledge> all = knowledgeMapper.selectList(w);
            status.put("documentCount", all.size());
            status.put("chunkCount", embeddingService.getChunkCount());
            status.put("embeddingDimension", 1024);
            status.put("lastUpdate", all.isEmpty() ? null : all.get(0).getCreateTime());
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            status.put("documentCount", 0);
            status.put("chunkCount", 0);
        }
        return Result.success(status);
    }

    public Knowledge uploadKnowledge(Long teacherId, Long kbId, MultipartFile file) throws IOException {
        checkKbOwnership(teacherId, kbId);
        log.info("========== uploadKnowledge 开始 ==========");
        log.info("收到文件: name={}, size={}", file.getOriginalFilename(), file.getSize());

        Path dirPath = Paths.get(uploadDir);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
            log.info("创建上传目录: {}", dirPath.toAbsolutePath());
        }

        // 安全文件名：用 UUID 重命名，保留原始扩展名，防止路径穿越
        String originalName = file.getOriginalFilename();
        String safeName = UUID.randomUUID().toString();
        if (originalName != null && originalName.contains(".")) {
            String ext = originalName.substring(originalName.lastIndexOf("."));
            safeName += ext;
        }
        Path filePath = dirPath.resolve(safeName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        long fileSize = Files.size(filePath);
        log.info("文件保存成功: path={}, size={}", filePath.toAbsolutePath(), fileSize);

        Knowledge knowledge = new Knowledge();
        knowledge.setKbId(kbId);
        knowledge.setFileName(originalName);
        knowledge.setFilePath(filePath.toAbsolutePath().toString());
        knowledge.setFileSize(fileSize);
        knowledge.setChunkCount(0);
        knowledge.setStatus(0);
        knowledge.setCreateTime(LocalDateTime.now());
        knowledgeMapper.insert(knowledge);
        log.info("知识记录已插入: id={}, kbId={}", knowledge.getId(), kbId);

        Long kid = knowledge.getId();
        String absPath = filePath.toAbsolutePath().toString();
        Thread processor = new Thread(() -> {
            try {
                log.info("========== 异步处理文档开始: id={}, path={} ==========", kid, absPath);
                embeddingService.processDocument(kid, absPath);
                log.info("========== 异步处理文档完成: id={} ==========", kid);
            } catch (Exception e) {
                log.error("========== 异步处理文档失败: id={} ==========", kid, e);
                try {
                    Knowledge k = knowledgeMapper.selectById(kid);
                    if (k != null) {
                        k.setStatus(2);
                        knowledgeMapper.updateById(k);
                        log.info("已更新知识记录状态为失败: id={}", kid);
                    }
                } catch (Exception ex) {
                    log.error("更新知识记录状态失败: id={}", kid, ex);
                }
            }
        }, "doc-processor-" + kid);
        processor.setUncaughtExceptionHandler((t, e) -> {
            log.error("未捕获异常 in thread {}: ", t.getName(), e);
            try {
                Knowledge k = knowledgeMapper.selectById(kid);
                if (k != null) {
                    k.setStatus(2);
                    knowledgeMapper.updateById(k);
                }
            } catch (Exception ignored) {}
        });
        processor.start();

        Thread watchdog = new Thread(() -> {
            try {
                Thread.sleep(3600000);
                if (processor.isAlive()) {
                    log.warn("========== 文档处理超时(60分钟), 强制中断: id={} ==========", kid);
                    processor.interrupt();
                    Knowledge k = knowledgeMapper.selectById(kid);
                    if (k != null && k.getStatus() == 0) {
                        k.setStatus(2);
                        knowledgeMapper.updateById(k);
                    }
                }
            } catch (InterruptedException ignored) {}
        }, "doc-watchdog-" + kid);
        watchdog.setDaemon(true);
        watchdog.start();

        log.info("========== uploadKnowledge 返回（后台处理中）==========");
        return knowledge;
    }

    public Knowledge importText(Long teacherId, Long kbId, String title, String content) {
        checkKbOwnership(teacherId, kbId);
        log.info("========== importText 开始: title={}, 内容长度={}, kbId={} ==========", title, content.length(), kbId);

        Knowledge knowledge = new Knowledge();
        knowledge.setKbId(kbId);
        knowledge.setFileName(title + ".txt");
        knowledge.setFilePath("text-import");
        knowledge.setFileSize((long) content.getBytes().length);
        knowledge.setChunkCount(0);
        knowledge.setStatus(0);
        knowledge.setCreateTime(LocalDateTime.now());
        knowledgeMapper.insert(knowledge);
        log.info("知识记录已插入: id={}", knowledge.getId());

        Long kid = knowledge.getId();
        Thread processor = new Thread(() -> {
            try {
                log.info("========== 异步处理文本导入开始: id={} ==========", kid);
                embeddingService.processText(kid, title, content);
                log.info("========== 异步处理文本导入完成: id={} ==========", kid);
            } catch (Exception e) {
                log.error("========== 异步处理文本导入失败: id={} ==========", kid, e);
                try {
                    Knowledge k = knowledgeMapper.selectById(kid);
                    if (k != null) {
                        k.setStatus(2);
                        knowledgeMapper.updateById(k);
                    }
                } catch (Exception ignored) {}
            }
        }, "text-processor-" + kid);
        processor.setUncaughtExceptionHandler((t, e) -> {
            log.error("未捕获异常 in thread {}: ", t.getName(), e);
            try {
                Knowledge k = knowledgeMapper.selectById(kid);
                if (k != null) {
                    k.setStatus(2);
                    knowledgeMapper.updateById(k);
                }
            } catch (Exception ignored) {}
        });
        processor.start();

        log.info("========== importText 返回（后台处理中）==========");
        return knowledge;
    }

    public Result<?> deleteKnowledge(Long id, Long teacherId) {
        Knowledge doc = knowledgeMapper.selectById(id);
        if (doc == null) return Result.error(404, "文档不存在");
        if (doc.getKbId() != null) {
            checkKbOwnership(teacherId, doc.getKbId());
        }
        embeddingService.removeDocument(id);
        // 删除物理文件（跳过文本导入的占位记录）
        if (doc.getFilePath() != null && !"text-import".equals(doc.getFilePath())) {
            try {
                Files.deleteIfExists(Paths.get(doc.getFilePath()));
            } catch (IOException e) {
                log.warn("删除物理文件失败: {}", doc.getFilePath(), e);
            }
        }
        knowledgeMapper.deleteById(id);
        return Result.success();
    }

    public void rebuildIndex() {
        embeddingService.rebuildIndex();
    }
}
