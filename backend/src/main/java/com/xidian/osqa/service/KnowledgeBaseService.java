package com.xidian.osqa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xidian.osqa.common.Result;
import com.xidian.osqa.entity.Knowledge;
import com.xidian.osqa.entity.KnowledgeBase;
import com.xidian.osqa.mapper.KnowledgeBaseMapper;
import com.xidian.osqa.mapper.KnowledgeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class KnowledgeBaseService {

    private final KnowledgeBaseMapper kbMapper;
    private final KnowledgeMapper knowledgeMapper;
    private final KnowledgeEmbeddingService embeddingService;

    public KnowledgeBaseService(KnowledgeBaseMapper kbMapper, KnowledgeMapper knowledgeMapper, KnowledgeEmbeddingService embeddingService) {
        this.kbMapper = kbMapper;
        this.knowledgeMapper = knowledgeMapper;
        this.embeddingService = embeddingService;
    }

    public Result<?> list(Long teacherId) {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBase::getTeacherId, teacherId);
        wrapper.orderByDesc(KnowledgeBase::getCreateTime);
        List<KnowledgeBase> list = kbMapper.selectList(wrapper);
        for (KnowledgeBase kb : list) {
            LambdaQueryWrapper<Knowledge> kw = new LambdaQueryWrapper<>();
            kw.eq(Knowledge::getKbId, kb.getId());
            kb.setDocumentCount(Math.toIntExact(knowledgeMapper.selectCount(kw)));
        }
        return Result.success(list);
    }

    public Result<?> create(Long teacherId, String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            return Result.error(400, "知识库名称不能为空");
        }
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName(name.trim());
        kb.setTeacherId(teacherId);
        kb.setDescription(description);
        kb.setCreateTime(LocalDateTime.now());
        kb.setUpdateTime(LocalDateTime.now());
        kbMapper.insert(kb);
        return Result.success(kb);
    }

    public Result<?> update(Long id, Long teacherId, String name, String description) {
        KnowledgeBase kb = kbMapper.selectById(id);
        if (kb == null || !kb.getTeacherId().equals(teacherId)) {
            return Result.error(403, "无权操作此知识库");
        }
        if (name != null && !name.trim().isEmpty()) {
            kb.setName(name.trim());
        }
        kb.setDescription(description);
        kb.setUpdateTime(LocalDateTime.now());
        kbMapper.updateById(kb);
        return Result.success();
    }

    @Transactional
    public Result<?> delete(Long id, Long teacherId) {
        KnowledgeBase kb = kbMapper.selectById(id);
        if (kb == null || !kb.getTeacherId().equals(teacherId)) {
            return Result.error(403, "无权操作此知识库");
        }
        // 删除该知识库下所有文档及其知识块
        LambdaQueryWrapper<Knowledge> kw = new LambdaQueryWrapper<>();
        kw.eq(Knowledge::getKbId, id);
        List<Knowledge> docs = knowledgeMapper.selectList(kw);
        for (Knowledge doc : docs) {
            embeddingService.removeDocument(doc.getId());
            knowledgeMapper.deleteById(doc.getId());
        }
        kbMapper.deleteById(id);
        return Result.success();
    }
}
