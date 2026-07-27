package com.xidian.osqa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xidian.osqa.common.Result;
import com.xidian.osqa.entity.ClassStudent;
import com.xidian.osqa.entity.Clazz;
import com.xidian.osqa.entity.User;
import com.xidian.osqa.entity.SysOption;
import com.xidian.osqa.mapper.ClassStudentMapper;
import com.xidian.osqa.mapper.ClazzMapper;
import com.xidian.osqa.mapper.UserMapper;
import com.xidian.osqa.mapper.SysOptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;

import com.xidian.osqa.entity.KnowledgeBase;
import com.xidian.osqa.entity.VideoSet;
import com.xidian.osqa.mapper.KnowledgeBaseMapper;
import com.xidian.osqa.mapper.VideoSetMapper;

import jakarta.servlet.http.HttpServletResponse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClazzService {

    private static final Logger log = LoggerFactory.getLogger(ClazzService.class);

    private final ClazzMapper clazzMapper;
    private final ClassStudentMapper classStudentMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final SysOptionMapper sysOptionMapper;
    private final VideoSetMapper videoSetMapper;
    private final KnowledgeBaseMapper knowledgeBaseMapper;

    public ClazzService(ClazzMapper clazzMapper, ClassStudentMapper classStudentMapper, UserMapper userMapper, PasswordEncoder passwordEncoder, SysOptionMapper sysOptionMapper, VideoSetMapper videoSetMapper, KnowledgeBaseMapper knowledgeBaseMapper) {
        this.clazzMapper = clazzMapper;
        this.classStudentMapper = classStudentMapper;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.sysOptionMapper = sysOptionMapper;
        this.videoSetMapper = videoSetMapper;
        this.knowledgeBaseMapper = knowledgeBaseMapper;
    }

    // ========== 教师端：班级CRUD ==========

    public Result<?> getClasses(Long teacherId) {
        LambdaQueryWrapper<Clazz> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Clazz::getTeacherId, teacherId);
        wrapper.orderByDesc(Clazz::getCreateTime);
        List<Clazz> classes = clazzMapper.selectList(wrapper);

        for (Clazz clazz : classes) {
            LambdaQueryWrapper<ClassStudent> csWrapper = new LambdaQueryWrapper<>();
            csWrapper.eq(ClassStudent::getClassId, clazz.getId());
            List<ClassStudent> csList = classStudentMapper.selectList(csWrapper);
            List<Long> studentIds = csList.stream().map(ClassStudent::getStudentId).collect(Collectors.toList());
            // 与 getClassStudents 一致：只统计未被逻辑删除的有效学生，避免“列表显示N人、点进去N-1人”
            int count = studentIds.isEmpty() ? 0 : userMapper.selectBatchIds(studentIds).size();
            clazz.setStudentCount(count);

            if (clazz.getVideoSetId() != null) {
                VideoSet vs = videoSetMapper.selectById(clazz.getVideoSetId());
                clazz.setVideoSetName(vs != null ? vs.getName() : null);
            }
            if (clazz.getKbId() != null) {
                KnowledgeBase kb = knowledgeBaseMapper.selectById(clazz.getKbId());
                clazz.setKbName(kb != null ? kb.getName() : null);
            }
        }

        return Result.success(classes);
    }

    public Result<?> createClass(Long teacherId, String name, String startTime, String endTime, Long videoSetId, Long kbId) {
        if (name == null || name.trim().isEmpty()) {
            return Result.error(400, "班级名称不能为空");
        }
        if (startTime == null || endTime == null) {
            return Result.error(400, "请设置班级起止时间");
        }

        // 校验视频集/知识库归属
        if (videoSetId != null) {
            VideoSet vs = videoSetMapper.selectById(videoSetId);
            if (vs == null || !vs.getTeacherId().equals(teacherId)) {
                return Result.error(403, "视频集不存在或无权使用");
            }
        }
        if (kbId != null) {
            KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
            if (kb == null || !kb.getTeacherId().equals(teacherId)) {
                return Result.error(403, "知识库不存在或无权使用");
            }
        }

        Clazz clazz = new Clazz();
        clazz.setName(name.trim());
        clazz.setTeacherId(teacherId);
        clazz.setVideoSetId(videoSetId);
        clazz.setKbId(kbId);
        clazz.setStartTime(LocalDateTime.parse(startTime.replace(" ", "T")));
        clazz.setEndTime(LocalDateTime.parse(endTime.replace(" ", "T")));
        clazz.setStatus(1);
        clazz.setCreateTime(LocalDateTime.now());
        clazz.setUpdateTime(LocalDateTime.now());
        clazzMapper.insert(clazz);

        return Result.success(clazz);
    }

    /**
     * 为已存在班级挂载/修改/取消挂载视频集与知识库。
     * 默认 updateById 不写 null，故用 LambdaUpdateWrapper.set 显式落库以支持取消挂载。
     */
    public Result<?> updateClassResources(Long classId, Long teacherId, Long videoSetId, Long kbId) {
        Clazz clazz = clazzMapper.selectById(classId);
        if (clazz == null) {
            return Result.error(404, "班级不存在");
        }
        if (!clazz.getTeacherId().equals(teacherId)) {
            return Result.error(403, "无权操作此班级");
        }

        // 校验视频集/知识库归属
        if (videoSetId != null) {
            VideoSet vs = videoSetMapper.selectById(videoSetId);
            if (vs == null || !vs.getTeacherId().equals(teacherId)) {
                return Result.error(403, "视频集不存在或无权使用");
            }
        }
        if (kbId != null) {
            KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
            if (kb == null || !kb.getTeacherId().equals(teacherId)) {
                return Result.error(403, "知识库不存在或无权使用");
            }
        }

        LambdaUpdateWrapper<Clazz> uw = new LambdaUpdateWrapper<>();
        uw.eq(Clazz::getId, classId)
                .set(Clazz::getVideoSetId, videoSetId)
                .set(Clazz::getKbId, kbId)
                .set(Clazz::getUpdateTime, LocalDateTime.now());
        clazzMapper.update(null, uw);

        // 回填名称给前端展示
        clazz.setVideoSetId(videoSetId);
        clazz.setKbId(kbId);
        if (videoSetId != null) {
            VideoSet vs = videoSetMapper.selectById(videoSetId);
            clazz.setVideoSetName(vs != null ? vs.getName() : null);
        } else {
            clazz.setVideoSetName(null);
        }
        if (kbId != null) {
            KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
            clazz.setKbName(kb != null ? kb.getName() : null);
        } else {
            clazz.setKbName(null);
        }
        return Result.success(clazz);
    }

    @Transactional
    public Result<?> deleteClass(Long classId, Long teacherId) {
        Clazz clazz = clazzMapper.selectById(classId);
        if (clazz == null) {
            return Result.error(404, "班级不存在");
        }
        if (!clazz.getTeacherId().equals(teacherId)) {
            return Result.error(403, "无权操作此班级");
        }

        // 删除班级学生关联
        LambdaQueryWrapper<ClassStudent> csWrapper = new LambdaQueryWrapper<>();
        csWrapper.eq(ClassStudent::getClassId, classId);
        classStudentMapper.delete(csWrapper);

        // 逻辑删除班级
        clazzMapper.deleteById(classId);
        return Result.success();
    }

    @Transactional
    public Result<?> dissolveClass(Long classId, Long teacherId) {
        Clazz clazz = clazzMapper.selectById(classId);
        if (clazz == null) {
            return Result.error(404, "班级不存在");
        }
        if (!clazz.getTeacherId().equals(teacherId)) {
            return Result.error(403, "无权操作此班级");
        }

        // 踢出所有学生
        LambdaQueryWrapper<ClassStudent> csWrapper = new LambdaQueryWrapper<>();
        csWrapper.eq(ClassStudent::getClassId, classId);
        classStudentMapper.delete(csWrapper);

        clazz.setStatus(0);
        clazz.setUpdateTime(LocalDateTime.now());
        clazzMapper.updateById(clazz);
        return Result.success();
    }

    // ========== 班级学生管理 ==========

    public Result<?> getClassStudents(Long classId) {
        LambdaQueryWrapper<ClassStudent> csWrapper = new LambdaQueryWrapper<>();
        csWrapper.eq(ClassStudent::getClassId, classId);
        List<ClassStudent> csList = classStudentMapper.selectList(csWrapper);

        List<Long> studentIds = csList.stream().map(ClassStudent::getStudentId).collect(Collectors.toList());
        if (studentIds.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        List<User> students = userMapper.selectBatchIds(studentIds);
        return Result.success(students);
    }

    public Result<?> addStudent(Long classId, Long studentId) {
        Clazz clazz = clazzMapper.selectById(classId);
        if (clazz == null) {
            return Result.error(404, "班级不存在");
        }
        if (clazz.getStatus() == 0) {
            return Result.error(400, "班级已解散");
        }

        User student = userMapper.selectById(studentId);
        if (student == null || !"STUDENT".equals(student.getRole())) {
            return Result.error(400, "用户不存在或不是学生");
        }

        // 检查是否已在班级中
        LambdaQueryWrapper<ClassStudent> csWrapper = new LambdaQueryWrapper<>();
        csWrapper.eq(ClassStudent::getClassId, classId);
        csWrapper.eq(ClassStudent::getStudentId, studentId);
        if (classStudentMapper.selectCount(csWrapper) > 0) {
            return Result.error(400, "该学生已在班级中");
        }

        ClassStudent cs = new ClassStudent();
        cs.setClassId(classId);
        cs.setStudentId(studentId);
        cs.setCreateTime(LocalDateTime.now());
        classStudentMapper.insert(cs);
        return Result.success();
    }

    public Result<?> removeStudent(Long classId, Long studentId) {
        LambdaQueryWrapper<ClassStudent> csWrapper = new LambdaQueryWrapper<>();
        csWrapper.eq(ClassStudent::getClassId, classId);
        csWrapper.eq(ClassStudent::getStudentId, studentId);
        classStudentMapper.delete(csWrapper);
        return Result.success();
    }

    public Result<?> addStudentsByUsernames(Long classId, List<String> usernames) {
        Clazz clazz = clazzMapper.selectById(classId);
        if (clazz == null) {
            return Result.error(404, "班级不存在");
        }

        int added = 0;
        int skipped = 0;
        for (String username : usernames) {
            LambdaQueryWrapper<User> uWrapper = new LambdaQueryWrapper<>();
            uWrapper.eq(User::getUsername, username.trim());
            uWrapper.eq(User::getRole, "STUDENT");
            User student = userMapper.selectOne(uWrapper);
            if (student == null) {
                skipped++;
                continue;
            }

            LambdaQueryWrapper<ClassStudent> csWrapper = new LambdaQueryWrapper<>();
            csWrapper.eq(ClassStudent::getClassId, classId);
            csWrapper.eq(ClassStudent::getStudentId, student.getId());
            if (classStudentMapper.selectCount(csWrapper) > 0) {
                skipped++;
                continue;
            }

            ClassStudent cs = new ClassStudent();
            cs.setClassId(classId);
            cs.setStudentId(student.getId());
            cs.setCreateTime(LocalDateTime.now());
            classStudentMapper.insert(cs);
            added++;
        }

        return Result.success(java.util.Map.of("added", added, "skipped", skipped));
    }

    // ========== 学生端：查询所在班级 ==========

    /**
     * 查询学生所在的所有活跃且未过期班级（学生可同时加入多个班级）
     */
    public List<Clazz> getStudentClasses(Long studentId) {
        LambdaQueryWrapper<ClassStudent> csWrapper = new LambdaQueryWrapper<>();
        csWrapper.eq(ClassStudent::getStudentId, studentId);
        List<ClassStudent> csList = classStudentMapper.selectList(csWrapper);

        List<Clazz> result = new ArrayList<>();
        for (ClassStudent cs : csList) {
            Clazz clazz = clazzMapper.selectById(cs.getClassId());
            if (clazz == null || clazz.getStatus() != 1) continue;
            if (clazz.getEndTime() == null || !clazz.getEndTime().isAfter(LocalDateTime.now())) continue;
            if (clazz.getVideoSetId() != null) {
                VideoSet vs = videoSetMapper.selectById(clazz.getVideoSetId());
                clazz.setVideoSetName(vs != null ? vs.getName() : null);
            }
            if (clazz.getKbId() != null) {
                KnowledgeBase kb = knowledgeBaseMapper.selectById(clazz.getKbId());
                clazz.setKbName(kb != null ? kb.getName() : null);
            }
            result.add(clazz);
        }
        return result;
    }

    /**
     * 校验学生是否在某班级中，返回该班级（含挂载的视频集/知识库信息），否则 null
     */
    public Clazz getClazzForStudent(Long classId, Long studentId) {
        LambdaQueryWrapper<ClassStudent> csWrapper = new LambdaQueryWrapper<>();
        csWrapper.eq(ClassStudent::getClassId, classId);
        csWrapper.eq(ClassStudent::getStudentId, studentId);
        if (classStudentMapper.selectCount(csWrapper) == 0) {
            return null;
        }
        Clazz clazz = clazzMapper.selectById(classId);
        if (clazz == null) return null;
        if (clazz.getVideoSetId() != null) {
            VideoSet vs = videoSetMapper.selectById(clazz.getVideoSetId());
            clazz.setVideoSetName(vs != null ? vs.getName() : null);
        }
        if (clazz.getKbId() != null) {
            KnowledgeBase kb = knowledgeBaseMapper.selectById(clazz.getKbId());
            clazz.setKbName(kb != null ? kb.getName() : null);
        }
        return clazz;
    }

    /**
     * 查询学生当前所在的活跃班级（返回首个，向后兼容）
     */
    public Clazz getStudentActiveClass(Long studentId) {
        List<Clazz> classes = getStudentClasses(studentId);
        return classes.isEmpty() ? null : classes.get(0);
    }

    /**
     * 自动解散过期班级（定时任务调用）
     */
    public int dissolveExpiredClasses() {
        LambdaQueryWrapper<Clazz> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Clazz::getStatus, 1);
        wrapper.le(Clazz::getEndTime, LocalDateTime.now());
        List<Clazz> expired = clazzMapper.selectList(wrapper);

        for (Clazz clazz : expired) {
            // 踢出所有学生
            LambdaQueryWrapper<ClassStudent> csWrapper = new LambdaQueryWrapper<>();
            csWrapper.eq(ClassStudent::getClassId, clazz.getId());
            classStudentMapper.delete(csWrapper);

            clazz.setStatus(0);
            clazz.setUpdateTime(LocalDateTime.now());
            clazzMapper.updateById(clazz);
            log.info("班级[{}]已到期自动解散: {}", clazz.getId(), clazz.getName());
        }

        return expired.size();
    }

    // ========== 班级内学生管理（替代原 StudentManage）==========

    /**
     * 在班级内创建学生（创建用户 + 加入班级）
     */
    public Result<?> createStudentInClass(Long classId, Map<String, String> body) {
        String studentId = body.get("studentId");
        String name = body.get("name");
        if (studentId == null || studentId.isBlank() || name == null || name.isBlank()) {
            return Result.error(400, "学号和姓名不能为空");
        }

        // 先查是否已有该学号账号：有则直接加入班级（支持一个学生进多个班级），无则新建
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, studentId.trim());
        User existing = userMapper.selectOne(wrapper);

        User user;
        if (existing != null) {
            if (!"STUDENT".equals(existing.getRole())) {
                return Result.error(400, "该学号不是学生账号，无法加入班级");
            }
            // 检查是否已在本班级
            LambdaQueryWrapper<ClassStudent> csCheck = new LambdaQueryWrapper<>();
            csCheck.eq(ClassStudent::getClassId, classId);
            csCheck.eq(ClassStudent::getStudentId, existing.getId());
            if (classStudentMapper.selectCount(csCheck) > 0) {
                return Result.error(400, "该学生已在班级中");
            }
            user = existing;
        } else {
            // 新建学生账号
            user = new User();
            user.setUsername(studentId.trim());
            String defaultPassword = studentId.length() >= 6 ? studentId.substring(studentId.length() - 6) : studentId;
            user.setPassword(passwordEncoder.encode(defaultPassword));
            user.setRealName(name.trim());
            user.setPhone(body.get("phone"));
            user.setCollege(body.get("college"));
            user.setMajor(body.get("major"));
            user.setGrade(body.get("grade"));
            user.setRole("STUDENT");
            user.setStatus(1);
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            userMapper.insert(user);
        }

        // 加入班级
        ClassStudent cs = new ClassStudent();
        cs.setClassId(classId);
        cs.setStudentId(user.getId());
        cs.setCreateTime(LocalDateTime.now());
        classStudentMapper.insert(cs);

        return Result.success(user);
    }

    /**
     * 批量导入学生到班级
     */
    public Result<?> importStudentsInClass(Long classId, MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            return Result.error(400, "文件为空");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            return Result.error(400, "请上传Excel文件（.xlsx或.xls）");
        }

        int[] counts = {0, 0, 0};
        Set<String> newColleges = new HashSet<>();
        Set<String> newMajors = new HashSet<>();
        Set<String> newGrades = new HashSet<>();

        EasyExcel.read(file.getInputStream(), new ReadListener<Map<Integer, String>>() {
            @Override
            public void invoke(Map<Integer, String> row, AnalysisContext context) {
                counts[0]++;
                try {
                    String studentId = row.get(0);
                    String name = row.get(1);
                    String college = row.size() > 2 ? row.get(2) : null;
                    String major = row.size() > 3 ? row.get(3) : null;
                    String grade = row.size() > 4 ? row.get(4) : null;

                    if (studentId == null || studentId.isBlank() || name == null || name.isBlank()) {
                        counts[2]++;
                        return;
                    }

                    studentId = studentId.trim();
                    name = name.trim();
                    if (college != null && !college.isBlank()) college = college.trim(); else college = null;
                    if (major != null && !major.isBlank()) major = major.trim(); else major = null;
                    if (grade != null && !grade.isBlank()) grade = grade.trim(); else grade = null;

                    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(User::getUsername, studentId);
                    User existing = userMapper.selectOne(wrapper);

                    Long userId;
                    if (existing != null) {
                        // 用户已存在，检查是否已在班级中
                        LambdaQueryWrapper<ClassStudent> csWrapper = new LambdaQueryWrapper<>();
                        csWrapper.eq(ClassStudent::getClassId, classId);
                        csWrapper.eq(ClassStudent::getStudentId, existing.getId());
                        if (classStudentMapper.selectCount(csWrapper) > 0) {
                            counts[2]++;
                            return;
                        }
                        userId = existing.getId();
                    } else {
                        // 创建新用户
                        User user = new User();
                        user.setUsername(studentId);
                        String defaultPassword = studentId.length() >= 6 ? studentId.substring(studentId.length() - 6) : studentId;
                        user.setPassword(passwordEncoder.encode(defaultPassword));
                        user.setRealName(name);
                        user.setCollege(college);
                        user.setMajor(major);
                        user.setGrade(grade);
                        user.setRole("STUDENT");
                        user.setStatus(1);
                        user.setCreateTime(LocalDateTime.now());
                        user.setUpdateTime(LocalDateTime.now());
                        userMapper.insert(user);
                        userId = user.getId();
                    }

                    // 加入班级
                    ClassStudent cs = new ClassStudent();
                    cs.setClassId(classId);
                    cs.setStudentId(userId);
                    cs.setCreateTime(LocalDateTime.now());
                    classStudentMapper.insert(cs);
                    counts[1]++;

                    if (college != null) newColleges.add(college);
                    if (major != null) newMajors.add(major);
                    if (grade != null) newGrades.add(grade);

                } catch (Exception e) {
                    counts[2]++;
                    log.warn("导入第{}行失败: {}", counts[0], e.getMessage());
                }
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
                log.info("班级导入完成: 总行数={}, 成功={}, 失败={}", counts[0], counts[1], counts[2]);
            }
        }).sheet().headRowNumber(1).doRead();

        autoUpdateOptions("college", newColleges);
        autoUpdateOptions("major", newMajors);
        autoUpdateOptions("grade", newGrades);

        Map<String, Object> result = new HashMap<>();
        result.put("total", counts[0]);
        result.put("success", counts[1]);
        result.put("failed", counts[2]);
        return Result.success(result);
    }

    private void autoUpdateOptions(String category, Set<String> values) {
        if (values == null || values.isEmpty()) return;
        for (String value : values) {
            LambdaQueryWrapper<SysOption> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysOption::getCategory, category).eq(SysOption::getOptionValue, value);
            if (sysOptionMapper.selectCount(wrapper) == 0) {
                SysOption option = new SysOption();
                option.setCategory(category);
                option.setOptionValue(value);
                option.setSortOrder(0);
                sysOptionMapper.insert(option);
            }
        }
    }

    public void downloadTemplate(HttpServletResponse response) throws Exception {
        String fileName = URLEncoder.encode("学生导入模板", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        List<List<String>> head = List.of(
                List.of("学号"),
                List.of("姓名"),
                List.of("学院"),
                List.of("专业"),
                List.of("年级")
        );

        EasyExcel.write(response.getOutputStream())
                .head(head)
                .sheet("学生导入")
                .doWrite(List.of());
    }
}
