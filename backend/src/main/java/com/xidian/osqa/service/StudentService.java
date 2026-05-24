package com.xidian.osqa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xidian.osqa.entity.User;
import com.xidian.osqa.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class StudentService {

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

    public User createStudent(String studentId, String name, String email) {
        User user = new User();
        user.setUsername(studentId);
        String defaultPassword = studentId.length() >= 6 ? studentId.substring(studentId.length() - 6) : studentId;
        user.setPassword(passwordEncoder.encode(defaultPassword));
        user.setRealName(name);
        user.setEmail(email);
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
}
