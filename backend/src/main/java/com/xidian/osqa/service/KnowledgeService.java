package com.xidian.osqa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xidian.osqa.entity.Knowledge;
import com.xidian.osqa.mapper.KnowledgeMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KnowledgeService {

    private final KnowledgeMapper knowledgeMapper;

    @Value("${knowledge.upload-dir}")
    private String uploadDir;

    public KnowledgeService(KnowledgeMapper knowledgeMapper) {
        this.knowledgeMapper = knowledgeMapper;
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
            status.put("chunkCount", all.stream().mapToInt(k -> k.getChunkCount() != null ? k.getChunkCount() : 0).sum());
            status.put("embeddingDimension", 384);
            status.put("lastUpdate", all.isEmpty() ? null : all.get(0).getCreateTime());
        } catch (Exception e) {
            status.put("documentCount", 0);
            status.put("chunkCount", 0);
        }
        return status;
    }

    public Knowledge uploadKnowledge(MultipartFile file) throws IOException {
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = file.getOriginalFilename();
        String filePath = uploadDir + File.separator + fileName;
        file.transferTo(new File(filePath));

        Knowledge knowledge = new Knowledge();
        knowledge.setFileName(fileName);
        knowledge.setFilePath(filePath);
        knowledge.setFileSize(file.getSize());
        knowledge.setChunkCount(0);
        knowledge.setStatus(1);
        knowledge.setCreateTime(LocalDateTime.now());
        knowledgeMapper.insert(knowledge);

        return knowledge;
    }

    public void deleteKnowledge(Long id) {
        knowledgeMapper.deleteById(id);
    }

    public void rebuildIndex() {
        // placeholder for index rebuild
    }
}
