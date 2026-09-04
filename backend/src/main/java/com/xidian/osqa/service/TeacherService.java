package com.xidian.osqa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xidian.osqa.common.Result;
import com.xidian.osqa.entity.*;
import com.xidian.osqa.mapper.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TeacherService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final ClazzMapper clazzMapper;
    private final ClassStudentMapper classStudentMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeMapper knowledgeMapper;
    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final VideoSetMapper videoSetMapper;
    private final ChapterMapper chapterMapper;
    private final SectionMapper sectionMapper;

    public TeacherService(UserMapper userMapper, PasswordEncoder passwordEncoder,
                          ClazzMapper clazzMapper, ClassStudentMapper classStudentMapper,
                          KnowledgeBaseMapper knowledgeBaseMapper, KnowledgeMapper knowledgeMapper,
                          KnowledgeChunkMapper knowledgeChunkMapper, VideoSetMapper videoSetMapper,
                          ChapterMapper chapterMapper, SectionMapper sectionMapper) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.clazzMapper = clazzMapper;
        this.classStudentMapper = classStudentMapper;
        this.knowledgeBaseMapper = knowledgeBaseMapper;
        this.knowledgeMapper = knowledgeMapper;
        this.knowledgeChunkMapper = knowledgeChunkMapper;
        this.videoSetMapper = videoSetMapper;
        this.chapterMapper = chapterMapper;
        this.sectionMapper = sectionMapper;
    }

    /** 教师列表（分页，可按工号/姓名搜索、按审核状态筛选） */
    public Page<User> getTeacherList(int page, int size, String keyword, Integer auditStatus) {
        Page<User> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRole, "TEACHER");
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(User::getUsername, keyword).or().like(User::getRealName, keyword));
        }
        if (auditStatus != null) {
            wrapper.eq(User::getAuditStatus, auditStatus);
        }
        wrapper.orderByDesc(User::getCreateTime);
        return userMapper.selectPage(pageParam, wrapper);
    }

    /** 审核教师：1=通过，2=拒绝 */
    public Result<?> auditTeacher(Long id, Integer auditStatus) {
        if (auditStatus == null || (auditStatus != 1 && auditStatus != 2)) {
            return Result.error(400, "审核状态不合法");
        }
        User teacher = requireTeacher(id);
        if (teacher == null) return Result.error(404, "教师不存在");
        teacher.setAuditStatus(auditStatus);
        teacher.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(teacher);
        return Result.success();
    }

    /** 禁用/启用教师 */
    public Result<?> toggleStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            return Result.error(400, "状态不合法");
        }
        User teacher = requireTeacher(id);
        if (teacher == null) return Result.error(404, "教师不存在");
        teacher.setStatus(status);
        teacher.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(teacher);
        return Result.success();
    }

    /** 重置教师密码为工号后6位 */
    public Result<?> resetPassword(Long id) {
        User teacher = requireTeacher(id);
        if (teacher == null) return Result.error(404, "教师不存在");
        String username = teacher.getUsername();
        String newPassword = username.length() >= 6
                ? username.substring(username.length() - 6)
                : String.format("%6s", username).replace(' ', '0');
        teacher.setPassword(passwordEncoder.encode(newPassword));
        teacher.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(teacher);
        return Result.success("密码已重置为工号后6位");
    }

    /** 设置教师的 API 体验时间段（null 表示取消体验权限） */
    public Result<?> setTrialPeriod(Long id, String startTime, String endTime) {
        User teacher = requireTeacher(id);
        if (teacher == null) return Result.error(404, "教师不存在");

        if (startTime == null || startTime.isBlank() || endTime == null || endTime.isBlank()) {
            // 清除体验权限
            teacher.setTrialStartTime(null);
            teacher.setTrialEndTime(null);
        } else {
            try {
                LocalDateTime start = LocalDateTime.parse(startTime.replace(" ", "T"));
                LocalDateTime end = LocalDateTime.parse(endTime.replace(" ", "T"));
                if (end.isBefore(start)) {
                    return Result.error(400, "结束时间不能早于开始时间");
                }
                teacher.setTrialStartTime(start);
                teacher.setTrialEndTime(end);
            } catch (Exception e) {
                return Result.error(400, "时间格式无效，请使用 yyyy-MM-ddTHH:mm:ss 格式");
            }
        }
        teacher.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(teacher);
        return Result.success();
    }

    /** 删除教师，并级联清理其名下学生、班级、知识库、视频集 */
    @Transactional
    public Result<?> deleteTeacher(Long id) {
        User teacher = requireTeacher(id);
        if (teacher == null) return Result.error(404, "教师不存在");

        // 1. 删除教师名下的学生（逻辑删除）
        LambdaQueryWrapper<User> studentWrapper = new LambdaQueryWrapper<>();
        studentWrapper.eq(User::getRole, "STUDENT").eq(User::getTeacherId, id);
        List<User> students = userMapper.selectList(studentWrapper);
        for (User s : students) {
            userMapper.deleteById(s.getId());
        }

        // 2. 删除教师名下的班级及其学生关联
        LambdaQueryWrapper<Clazz> clazzWrapper = new LambdaQueryWrapper<>();
        clazzWrapper.eq(Clazz::getTeacherId, id);
        List<Clazz> classes = clazzMapper.selectList(clazzWrapper);
        for (Clazz c : classes) {
            LambdaQueryWrapper<ClassStudent> csWrapper = new LambdaQueryWrapper<>();
            csWrapper.eq(ClassStudent::getClassId, c.getId());
            classStudentMapper.delete(csWrapper);
            clazzMapper.deleteById(c.getId());
        }

        // 3. 删除教师名下的知识库及其文档、知识块
        LambdaQueryWrapper<KnowledgeBase> kbWrapper = new LambdaQueryWrapper<>();
        kbWrapper.eq(KnowledgeBase::getTeacherId, id);
        List<KnowledgeBase> kbs = knowledgeBaseMapper.selectList(kbWrapper);
        for (KnowledgeBase kb : kbs) {
            LambdaQueryWrapper<Knowledge> knowledgeWrapper = new LambdaQueryWrapper<>();
            knowledgeWrapper.eq(Knowledge::getKbId, kb.getId());
            List<Knowledge> docs = knowledgeMapper.selectList(knowledgeWrapper);
            for (Knowledge doc : docs) {
                LambdaQueryWrapper<KnowledgeChunk> chunkWrapper = new LambdaQueryWrapper<>();
                chunkWrapper.eq(KnowledgeChunk::getKnowledgeId, doc.getId());
                knowledgeChunkMapper.delete(chunkWrapper);
                knowledgeMapper.deleteById(doc.getId());
            }
            knowledgeBaseMapper.deleteById(kb.getId());
        }

        // 4. 删除教师名下的视频集及其章节、小节
        LambdaQueryWrapper<VideoSet> vsWrapper = new LambdaQueryWrapper<>();
        vsWrapper.eq(VideoSet::getTeacherId, id);
        List<VideoSet> videoSets = videoSetMapper.selectList(vsWrapper);
        for (VideoSet vs : videoSets) {
            LambdaQueryWrapper<Chapter> chapterWrapper = new LambdaQueryWrapper<>();
            chapterWrapper.eq(Chapter::getVideoSetId, vs.getId());
            List<Chapter> chapters = chapterMapper.selectList(chapterWrapper);
            for (Chapter chapter : chapters) {
                LambdaQueryWrapper<Section> sectionWrapper = new LambdaQueryWrapper<>();
                sectionWrapper.eq(Section::getChapterId, chapter.getId());
                sectionMapper.delete(sectionWrapper);
                chapterMapper.deleteById(chapter.getId());
            }
            videoSetMapper.deleteById(vs.getId());
        }

        // 5. 删除教师账号（逻辑删除）
        userMapper.deleteById(id);
        return Result.success();
    }

    private User requireTeacher(Long id) {
        if (id == null) return null;
        User user = userMapper.selectById(id);
        if (user == null || !"TEACHER".equals(user.getRole())) return null;
        return user;
    }
}
