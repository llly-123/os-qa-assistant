package com.xidian.osqa.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xidian.osqa.entity.User;
import com.xidian.osqa.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class StudentService {

    private static final Logger log = LoggerFactory.getLogger(StudentService.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public StudentService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public Page<User> getStudentList(int page, int size, String keyword) {
        Page<User> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRole, "STUDENT");
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(User::getUsername, keyword).or().like(User::getRealName, keyword));
        }
        wrapper.orderByDesc(User::getCreateTime);
        return userMapper.selectPage(pageParam, wrapper);
    }

    public User createStudent(String studentId, String name, String phone, String college, String major, String grade) {
        User user = new User();
        user.setUsername(studentId);
        String defaultPassword = studentId.length() >= 6 ? studentId.substring(studentId.length() - 6) : studentId;
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
        return user;
    }

    public Map<String, Object> resetPassword(Long studentId) {
        User user = userMapper.selectById(studentId);
        if (user == null) {
            return null;
        }
        String username = user.getUsername();
        String newPassword = username.length() >= 6 ? username.substring(username.length() - 6) : username;
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

        EasyExcel.read(file.getInputStream(), new ReadListener<Map<Integer, String>>() {
            @Override
            public void invoke(Map<Integer, String> row, AnalysisContext context) {
                counts[0]++;
                try {
                    String studentId = row.get(0);
                    String name = row.get(1);
                    String phone = row.size() > 2 ? row.get(2) : null;
                    String college = row.size() > 3 ? row.get(3) : null;
                    String major = row.size() > 4 ? row.get(4) : null;
                    String grade = row.size() > 5 ? row.get(5) : null;

                    if (studentId == null || studentId.isBlank() || name == null || name.isBlank()) {
                        counts[2]++;
                        log.warn("跳过空行: 第{}行", counts[0]);
                        return;
                    }

                    studentId = studentId.trim();
                    name = name.trim();
                    if (phone != null) phone = phone.trim();
                    if (college != null) college = college.trim();
                    if (major != null) major = major.trim();
                    if (grade != null) grade = grade.trim();

                    LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(User::getUsername, studentId);
                    if (userMapper.selectCount(wrapper) > 0) {
                        counts[2]++;
                        log.warn("学号已存在，跳过: {}", studentId);
                        return;
                    }

                    createStudent(studentId, name, phone, college, major, grade);
                    counts[1]++;
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

        Map<String, Object> result = new HashMap<>();
        result.put("total", counts[0]);
        result.put("success", counts[1]);
        result.put("failed", counts[2]);
        return result;
    }
}
