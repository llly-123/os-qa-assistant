package com.xidian.osqa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xidian.osqa.common.JwtUtil;
import com.xidian.osqa.common.Result;
import com.xidian.osqa.entity.User;
import com.xidian.osqa.mapper.UserMapper;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JavaMailSender mailSender;

    private final Map<String, String> codeStore = new ConcurrentHashMap<>();
    private final Map<String, Long> codeExpiry = new ConcurrentHashMap<>();

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, JavaMailSender mailSender) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.mailSender = mailSender;
    }

    public Result<?> login(String username, String password) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            return Result.error(401, "用户不存在");
        }

        if (user.getStatus() != 1) {
            return Result.error(403, "账号已被冻结");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return Result.error(401, "密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("realName", user.getRealName());
        userInfo.put("role", user.getRole());
        userInfo.put("email", user.getEmail());
        result.put("user", userInfo);

        return Result.success(result);
    }

    public Result<?> getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("realName", user.getRealName());
        userInfo.put("role", user.getRole());
        userInfo.put("email", user.getEmail());

        return Result.success(userInfo);
    }

    public Result<?> changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return Result.error(400, "原密码错误");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        return Result.success();
    }

    public Result<?> sendVerifyCode(String email) {
        String code = String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
        codeStore.put(email, code);
        codeExpiry.put(email, System.currentTimeMillis() + 5 * 60 * 1000);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("your_email@qq.com");
            message.setTo(email);
            message.setSubject("操作系统AI答疑助手 - 验证码");
            message.setText("您的验证码是：" + code + "，5分钟内有效。");
            mailSender.send(message);
        } catch (Exception e) {
            return Result.error("邮件发送失败，请检查邮箱地址");
        }

        return Result.success();
    }

    public Result<?> resetPassword(String email, String code) {
        String storedCode = codeStore.get(email);
        Long expiry = codeExpiry.get(email);

        if (storedCode == null || !storedCode.equals(code)) {
            return Result.error(400, "验证码错误");
        }

        if (expiry == null || System.currentTimeMillis() > expiry) {
            codeStore.remove(email);
            codeExpiry.remove(email);
            return Result.error(400, "验证码已过期");
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email);
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            return Result.error(404, "该邮箱未绑定任何账号");
        }

        String newPassword = user.getUsername().length() >= 6
                ? user.getUsername().substring(user.getUsername().length() - 6)
                : user.getUsername();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        codeStore.remove(email);
        codeExpiry.remove(email);

        Map<String, Object> result = new HashMap<>();
        result.put("newPassword", newPassword);
        return Result.success(result);
    }
}
