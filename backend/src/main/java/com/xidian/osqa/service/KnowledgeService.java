package com.xidian.osqa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xidian.osqa.entity.Knowledge;
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

@Service
public class KnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeService.class);

    private final KnowledgeMapper knowledgeMapper;
    private final KnowledgeEmbeddingService embeddingService;

    @Value("${knowledge.upload-dir}")
    private String uploadDir;

    public KnowledgeService(KnowledgeMapper knowledgeMapper, KnowledgeEmbeddingService embeddingService) {
        this.knowledgeMapper = knowledgeMapper;
        this.embeddingService = embeddingService;
    }

    public List<Knowledge> getKnowledgeList() {
        LambdaQueryWrapper<Knowledge> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Knowledge::getCreateTime);
        return knowledgeMapper.selectList(wrapper);
    }

    public Map<String, Object> getKnowledgeStatus() {
        Map<String, Object> status = new HashMap<>();
        try {
            List<Knowledge> all = knowledgeMapper.selectList(null);
            status.put("documentCount", all.size());
            status.put("chunkCount", embeddingService.getChunkCount());
            status.put("embeddingDimension", 1024);
            status.put("lastUpdate", all.isEmpty() ? null : all.get(0).getCreateTime());
        } catch (Exception e) {
            status.put("documentCount", 0);
            status.put("chunkCount", 0);
        }
        return status;
    }

    public Knowledge uploadKnowledge(MultipartFile file) throws IOException {
        log.info("========== uploadKnowledge 开始 ==========");
        log.info("收到文件: name={}, size={}", file.getOriginalFilename(), file.getSize());

        Path dirPath = Paths.get(uploadDir);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
            log.info("创建上传目录: {}", dirPath.toAbsolutePath());
        }

        String fileName = file.getOriginalFilename();
        Path filePath = dirPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        long fileSize = Files.size(filePath);
        log.info("文件保存成功: path={}, size={}", filePath.toAbsolutePath(), fileSize);

        Knowledge knowledge = new Knowledge();
        knowledge.setFileName(fileName);
        knowledge.setFilePath(filePath.toAbsolutePath().toString());
        knowledge.setFileSize(fileSize);
        knowledge.setChunkCount(0);
        knowledge.setStatus(0);
        knowledge.setCreateTime(LocalDateTime.now());
        knowledgeMapper.insert(knowledge);
        log.info("知识记录已插入: id={}", knowledge.getId());

        try {
            log.info("========== 开始同步处理文档 ==========");
            embeddingService.processDocument(knowledge.getId(), filePath.toAbsolutePath().toString());
            log.info("========== 同步处理文档完成 ==========");
        } catch (Exception e) {
            log.error("========== 同步处理文档失败 ==========", e);
            knowledge.setStatus(2);
            knowledgeMapper.updateById(knowledge);
            throw new RuntimeException("文档处理失败: " + e.getMessage(), e);
        }

        log.info("========== uploadKnowledge 完成 ==========");
        return knowledge;
    }

    public void deleteKnowledge(Long id) {
        embeddingService.removeDocument(id);
        knowledgeMapper.deleteById(id);
    }

    public void rebuildIndex() {
        embeddingService.rebuildIndex();
    }
}
