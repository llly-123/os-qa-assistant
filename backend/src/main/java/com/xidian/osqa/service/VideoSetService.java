package com.xidian.osqa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xidian.osqa.common.Result;
import com.xidian.osqa.entity.Chapter;
import com.xidian.osqa.entity.Section;
import com.xidian.osqa.entity.VideoSet;
import com.xidian.osqa.mapper.ChapterMapper;
import com.xidian.osqa.mapper.SectionMapper;
import com.xidian.osqa.mapper.VideoSetMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VideoSetService {

    private final VideoSetMapper videoSetMapper;
    private final ChapterMapper chapterMapper;
    private final SectionMapper sectionMapper;
    private final VideoService videoService;

    public VideoSetService(VideoSetMapper videoSetMapper, ChapterMapper chapterMapper, SectionMapper sectionMapper, VideoService videoService) {
        this.videoSetMapper = videoSetMapper;
        this.chapterMapper = chapterMapper;
        this.sectionMapper = sectionMapper;
        this.videoService = videoService;
    }

    public Result<?> list(Long teacherId) {
        LambdaQueryWrapper<VideoSet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VideoSet::getTeacherId, teacherId);
        wrapper.orderByDesc(VideoSet::getCreateTime);
        List<VideoSet> list = videoSetMapper.selectList(wrapper);
        for (VideoSet vs : list) {
            LambdaQueryWrapper<Chapter> cw = new LambdaQueryWrapper<>();
            cw.eq(Chapter::getVideoSetId, vs.getId());
            vs.setChapterCount(Math.toIntExact(chapterMapper.selectCount(cw)));
        }
        return Result.success(list);
    }

    public Result<?> create(Long teacherId, String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            return Result.error(400, "视频集名称不能为空");
        }
        VideoSet vs = new VideoSet();
        vs.setName(name.trim());
        vs.setTeacherId(teacherId);
        vs.setDescription(description);
        vs.setCreateTime(LocalDateTime.now());
        vs.setUpdateTime(LocalDateTime.now());
        videoSetMapper.insert(vs);
        return Result.success(vs);
    }

    public Result<?> update(Long id, Long teacherId, String name, String description) {
        VideoSet vs = videoSetMapper.selectById(id);
        if (vs == null || !vs.getTeacherId().equals(teacherId)) {
            return Result.error(403, "无权操作此视频集");
        }
        if (name != null && !name.trim().isEmpty()) {
            vs.setName(name.trim());
        }
        vs.setDescription(description);
        vs.setUpdateTime(LocalDateTime.now());
        videoSetMapper.updateById(vs);
        return Result.success();
    }

    @Transactional
    public Result<?> delete(Long id, Long teacherId) {
        VideoSet vs = videoSetMapper.selectById(id);
        if (vs == null || !vs.getTeacherId().equals(teacherId)) {
            return Result.error(403, "无权操作此视频集");
        }
        // 删除该视频集下所有章/节
        LambdaQueryWrapper<Chapter> cw = new LambdaQueryWrapper<>();
        cw.eq(Chapter::getVideoSetId, id);
        List<Chapter> chapters = chapterMapper.selectList(cw);
        for (Chapter chapter : chapters) {
            LambdaQueryWrapper<Section> sw = new LambdaQueryWrapper<>();
            sw.eq(Section::getChapterId, chapter.getId());
            sectionMapper.delete(sw);
            chapterMapper.deleteById(chapter.getId());
        }
        videoSetMapper.deleteById(id);
        return Result.success();
    }

    /** 取视频集下的章/节（供教师管理与学生查看复用） */
    public Result<?> chaptersOf(Long videoSetId) {
        return videoService.getChapters(videoSetId);
    }
}
