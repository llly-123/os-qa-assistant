package com.xidian.osqa.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xidian.osqa.common.Result;
import com.xidian.osqa.entity.User;
import com.xidian.osqa.entity.SysOption;
import com.xidian.osqa.mapper.UserMapper;
import com.xidian.osqa.mapper.ChatMessageMapper;
import com.xidian.osqa.mapper.SysOptionMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class StudentService {

    private static final Logger log = LoggerFactory.getLogger(StudentService.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final ChatMessageMapper chatMessageMapper;
    private final SysOptionMapper sysOptionMapper;

    public StudentService(UserMapper userMapper, PasswordEncoder passwordEncoder, ChatMessageMapper chatMessageMapper, SysOptionMapper sysOptionMapper) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.chatMessageMapper = chatMessageMapper;
        this.sysOptionMapper = sysOptionMapper;
    }

    public Page<User> getStudentList(int page, int size, String keyword, String college, Integer status) {
        Page<User> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRole, "STUDENT");
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(User::getUsername, keyword).or().like(User::getRealName, keyword));
        }
        if (college != null && !college.isEmpty()) {
            wrapper.eq(User::getCollege, college);
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        wrapper.orderByDesc(User::getCreateTime);
        Page<User> result = userMapper.selectPage(pageParam, wrapper);

        // 获取所有学生的最近提问时间
        try {
            List<Map<String, Object>> lastTimes = chatMessageMapper.findAllLastQuestionTimes();
            Map<Long, String> timeMap = new HashMap<>();
            for (Map<String, Object> item : lastTimes) {
                // 兼容不同数据库字段名大小写
                Object userIdObj = item.get("userId") != null ? item.get("userId") : item.get("USERID");
                Object timeObj = item.get("lastQuestionTime") != null ? item.get("lastQuestionTime") : item.get("LASTQUESTIONTIME");
                if (userIdObj != null && timeObj != null) {
                    Long userId;
                    if (userIdObj instanceof Number) {
                        userId = ((Number) userIdObj).longValue();
                    } else {
                        try {
                            userId = Long.parseLong(userIdObj.toString());
                        } catch (Exception e) {
                            log.warn("解析userId失败: {}", userIdObj);
                            continue;
                        }
                    }
                    timeMap.put(userId, timeObj.toString());
                }
            }
            log.info("查询到{}个学生的最近提问时间", timeMap.size());
            // 将最近提问时间附加到每个学生记录
            for (User user : result.getRecords()) {
                String lastTime = timeMap.get(user.getId());
                user.setExtraInfo(lastTime);
            }
        } catch (Exception e) {
            log.warn("获取最近提问时间失败", e);
        }

        return result;
    }

    // 获取所有学生（不分页，仅返回基础字段，用于班级管理勾选）
    public List<User> getAllStudents() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRole, "STUDENT")
               .eq(User::getStatus, 1)
               .orderByDesc(User::getCreateTime);
        return userMapper.selectList(wrapper);
    }

    public Result<?> createStudent(String studentId, String name, String phone, String college, String major, String grade) {
        if (studentId == null || studentId.isBlank()) {
            return Result.error(400, "学号不能为空");
        }
        // 学号唯一校验，避免重复创建学生账号
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, studentId.trim());
        if (userMapper.selectCount(wrapper) > 0) {
            return Result.error(400, "学号已存在");
        }
        User user = new User();
        user.setUsername(studentId.trim());
        String defaultPassword = studentId.length() >= 6 ? studentId.substring(studentId.length() - 6) : String.format("%6s", studentId).replace(' ', '0');
        user.setPassword(passwordEncoder.encode(defaultPassword));
        user.setRealName(name);
        user.setPhone(phone);
        user.setCollege(college);
        user.setMajor(major);
        user.setGrade(grade);
        user.setRole("STUDENT");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return Result.success(user);
    }

    public Map<String, Object> resetPassword(Long studentId) {
        User user = userMapper.selectById(studentId);
        if (user == null) {
            return null;
        }
        String username = user.getUsername();
        String newPassword = username.length() >= 6 ? username.substring(username.length() - 6) : String.format("%6s", username).replace(' ', '0');
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        Map<String, Object> result = new HashMap<>();
        result.put("newPassword", newPassword);
        return result;
    }

    public void toggleStatus(Long studentId, Integer status) {
        User user = userMapper.selectById(studentId);
        if (user != null) {
            user.setStatus(status);
            user.setUpdateTime(LocalDateTime.now());
            userMapper.updateById(user);
        }
    }

    public void deleteStudent(Long studentId) {
        userMapper.deleteById(studentId);
    }

    public void updateStudent(Long id, Map<String, String> body) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("学生不存在");
        }
        if (body.containsKey("realName")) user.setRealName(body.get("realName"));
        if (body.containsKey("college")) user.setCollege(body.get("college"));
        if (body.containsKey("major")) user.setMajor(body.get("major"));
        if (body.containsKey("grade")) user.setGrade(body.get("grade"));
        if (body.containsKey("phone")) user.setPhone(body.get("phone"));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    public Map<String, Object> batchImport(MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new RuntimeException("文件为空");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            throw new RuntimeException("请上传Excel文件（.xlsx或.xls）");
        }

        int[] counts = {0, 0, 0};
        // 收集新发现的选项值，用于自动更新
        Set<String> newColleges = new HashSet<>();
        Set<String> newMajors = new HashSet<>();
        Set<String> newGrades = new HashSet<>();

        EasyExcel.read(file.getInputStream(), new ReadListener<Map<Integer, String>>() {
            @Override
            public void invoke(Map<Integer, String> row, AnalysisContext context) {
                counts[0]++;
                try {
                    // 修正字段映射：学号(0)、姓名(1)、学院(2)、专业(3)、年级(4)
                    String studentId = row.get(0);
                    String name = row.get(1);
                    String college = row.size() > 2 ? row.get(2) : null;
                    String major = row.size() > 3 ? row.get(3) : null;
                    String grade = row.size() > 4 ? row.get(4) : null;

                    if (studentId == null || studentId.isBlank() || name == null || name.isBlank()) {
                        counts[2]++;
                        log.warn("跳过空行: 第{}行", counts[0]);
                        return;
                    }

                    studentId = studentId.trim();
                    name = name.trim();
                    if (college != null && !college.isBlank()) college = college.trim(); else college = null;
                    if (major != null && !major.isBlank()) major = major.trim(); else major = null;
                    if (grade != null && !grade.isBlank()) grade = grade.trim(); else grade = null;

                    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(User::getUsername, studentId);
                    if (userMapper.selectCount(wrapper) > 0) {
                        counts[2]++;
                        log.warn("学号已存在，跳过: {}", studentId);
                        return;
                    }

                    createStudent(studentId, name, null, college, major, grade);
                    counts[1]++;

                    // 收集新选项
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
                log.info("Excel解析完成: 总行数={}, 成功={}, 失败={}", counts[0], counts[1], counts[2]);
            }
        }).sheet().headRowNumber(1).doRead();

        // 自动更新选项设置（跳过已存在的）
        autoUpdateOptions("college", newColleges);
        autoUpdateOptions("major", newMajors);
        autoUpdateOptions("grade", newGrades);

        Map<String, Object> result = new HashMap<>();
        result.put("total", counts[0]);
        result.put("success", counts[1]);
        result.put("failed", counts[2]);
        return result;
    }

    /**
     * 自动更新选项设置，跳过已存在的值
     */
    private void autoUpdateOptions(String category, Set<String> values) {
        if (values == null || values.isEmpty()) return;
        int added = 0;
        for (String value : values) {
            LambdaQueryWrapper<SysOption> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysOption::getCategory, category).eq(SysOption::getOptionValue, value);
            if (sysOptionMapper.selectCount(wrapper) == 0) {
                SysOption option = new SysOption();
                option.setCategory(category);
                option.setOptionValue(value);
                option.setSortOrder(0);
                sysOptionMapper.insert(option);
                added++;
            }
        }
        if (added > 0) {
            log.info("自动新增{}个{}选项: {}", added, category, values);
        }
    }

    public void downloadTemplate(HttpServletResponse response) throws Exception {
        // 设置响应头
        String fileName = URLEncoder.encode("学生导入模板", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        // 表头
        List<List<String>> head = List.of(
                List.of("学号"),
                List.of("姓名"),
                List.of("学院"),
                List.of("专业"),
                List.of("年级")
        );

        // 写入Excel（只写表头，无数据行）
        EasyExcel.write(response.getOutputStream())
                .head(head)
                .sheet("学生导入")
                .doWrite(List.of());
    }
}
