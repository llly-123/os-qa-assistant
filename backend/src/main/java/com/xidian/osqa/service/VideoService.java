package com.xidian.osqa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xidian.osqa.common.Result;
import com.xidian.osqa.entity.Chapter;
import com.xidian.osqa.entity.Section;
import com.xidian.osqa.mapper.ChapterMapper;
import com.xidian.osqa.mapper.SectionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VideoService {

    private final ChapterMapper chapterMapper;
    private final SectionMapper sectionMapper;

    public VideoService(ChapterMapper chapterMapper, SectionMapper sectionMapper) {
        this.chapterMapper = chapterMapper;
        this.sectionMapper = sectionMapper;
    }

    // ========== Chapter CRUD ==========

    public Result<?> getChapters() {
        LambdaQueryWrapper<Chapter> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Chapter::getSortOrder);
        List<Chapter> chapters = chapterMapper.selectList(wrapper);

        for (Chapter chapter : chapters) {
            LambdaQueryWrapper<Section> sWrapper = new LambdaQueryWrapper<>();
            sWrapper.eq(Section::getChapterId, chapter.getId());
            sWrapper.orderByAsc(Section::getSortOrder);
            chapter.setSections(sectionMapper.selectList(sWrapper));
        }

        return Result.success(chapters);
    }

    public Result<?> addChapter(String title) {
        if (title == null || title.trim().isEmpty()) {
            return Result.error(400, "章标题不能为空");
        }

        Long maxOrder = chapterMapper.selectCount(null);
        Chapter chapter = new Chapter();
        chapter.setTitle(title.trim());
        chapter.setSortOrder(maxOrder.intValue());
        chapter.setCreateTime(LocalDateTime.now());
        chapter.setUpdateTime(LocalDateTime.now());
        chapterMapper.insert(chapter);

        return Result.success(chapter);
    }

    public Result<?> updateChapter(Long id, String title) {
        Chapter chapter = chapterMapper.selectById(id);
        if (chapter == null) {
            return Result.error(404, "章不存在");
        }
        chapter.setTitle(title);
        chapter.setUpdateTime(LocalDateTime.now());
        chapterMapper.updateById(chapter);
        return Result.success();
    }

    @Transactional
    public Result<?> deleteChapter(Long id) {
        // 删除该章下所有节
        LambdaQueryWrapper<Section> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Section::getChapterId, id);
        sectionMapper.delete(wrapper);
        chapterMapper.deleteById(id);
        return Result.success();
    }

    public Result<?> moveChapter(Long id, String direction) {
        Chapter chapter = chapterMapper.selectById(id);
        if (chapter == null) {
            return Result.error(404, "章不存在");
        }

        LambdaQueryWrapper<Chapter> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Chapter::getSortOrder);
        List<Chapter> all = chapterMapper.selectList(wrapper);

        int idx = -1;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(id)) { idx = i; break; }
        }

        int targetIdx = "up".equals(direction) ? idx - 1 : idx + 1;
        if (targetIdx < 0 || targetIdx >= all.size()) {
            return Result.error(400, "无法移动");
        }

        Chapter other = all.get(targetIdx);
        int tmpOrder = chapter.getSortOrder();
        chapter.setSortOrder(other.getSortOrder());
        other.setSortOrder(tmpOrder);
        chapter.setUpdateTime(LocalDateTime.now());
        other.setUpdateTime(LocalDateTime.now());
        chapterMapper.updateById(chapter);
        chapterMapper.updateById(other);

        return Result.success();
    }

    // ========== Section CRUD ==========

    public Result<?> addSection(Long chapterId, String title) {
        Chapter chapter = chapterMapper.selectById(chapterId);
        if (chapter == null) {
            return Result.error(404, "章不存在");
        }

        LambdaQueryWrapper<Section> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(Section::getChapterId, chapterId);
        Long count = sectionMapper.selectCount(countWrapper);

        Section section = new Section();
        section.setChapterId(chapterId);
        section.setTitle(title.trim());
        section.setSortOrder(count.intValue());
        section.setCreateTime(LocalDateTime.now());
        section.setUpdateTime(LocalDateTime.now());
        sectionMapper.insert(section);

        return Result.success(section);
    }

    public Result<?> updateSection(Long id, String title) {
        Section section = sectionMapper.selectById(id);
        if (section == null) {
            return Result.error(404, "节不存在");
        }
        section.setTitle(title);
        section.setUpdateTime(LocalDateTime.now());
        sectionMapper.updateById(section);
        return Result.success();
    }

    public Result<?> deleteSection(Long id) {
        sectionMapper.deleteById(id);
        return Result.success();
    }

    public Result<?> moveSection(Long id, String direction) {
        Section section = sectionMapper.selectById(id);
        if (section == null) {
            return Result.error(404, "节不存在");
        }

        LambdaQueryWrapper<Section> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Section::getChapterId, section.getChapterId());
        wrapper.orderByAsc(Section::getSortOrder);
        List<Section> all = sectionMapper.selectList(wrapper);

        int idx = -1;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(id)) { idx = i; break; }
        }

        int targetIdx = "up".equals(direction) ? idx - 1 : idx + 1;
        if (targetIdx < 0 || targetIdx >= all.size()) {
            return Result.error(400, "无法移动");
        }

        Section other = all.get(targetIdx);
        int tmpOrder = section.getSortOrder();
        section.setSortOrder(other.getSortOrder());
        other.setSortOrder(tmpOrder);
        section.setUpdateTime(LocalDateTime.now());
        other.setUpdateTime(LocalDateTime.now());
        sectionMapper.updateById(section);
        sectionMapper.updateById(other);

        return Result.success();
    }

    public Result<?> updateSectionVideo(Long sectionId, String videoUrl, Long videoSize, Integer videoDuration) {
        Section section = sectionMapper.selectById(sectionId);
        if (section == null) {
            return Result.error(404, "节不存在");
        }
        section.setVideoUrl(videoUrl);
        section.setVideoSize(videoSize);
        section.setVideoDuration(videoDuration);
        section.setUpdateTime(LocalDateTime.now());
        sectionMapper.updateById(section);
        return Result.success();
    }

    public Result<?> deleteSectionVideo(Long sectionId) {
        Section section = sectionMapper.selectById(sectionId);
        if (section == null) {
            return Result.error(404, "节不存在");
        }
        section.setVideoUrl(null);
        section.setVideoSize(0L);
        section.setVideoDuration(0);
        section.setUpdateTime(LocalDateTime.now());
        sectionMapper.updateById(section);
        return Result.success();
    }
}
