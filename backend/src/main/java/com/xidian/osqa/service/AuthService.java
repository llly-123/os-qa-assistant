package com.xidian.osqa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xidian.osqa.common.JwtUtil;
import com.xidian.osqa.common.Result;
import com.xidian.osqa.entity.User;
import com.xidian.osqa.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SmsService smsService;

    // tokenVersion: userId -> 当前有效版本号（密码重置时递增，旧token即刻失效）
    private final Map<Long, Long> tokenVersions = new ConcurrentHashMap<>();

    private final Map<String, String> codeStore = new ConcurrentHashMap<>();
    private final Map<String, Long> codeExpiry = new ConcurrentHashMap<>();

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, SmsService smsService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.smsService = smsService;
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

        // 教师账号需审核通过后才能登录
        if ("TEACHER".equals(user.getRole())) {
            Integer audit = user.getAuditStatus();
            if (audit == null || audit == 0) {
                return Result.error(403, "账号待审核，请等待管理员审核");
            }
            if (audit == 2) {
                return Result.error(403, "账号审核未通过");
            }
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            return Result.error(401, "密码错误");
        }

        long tokenVersion = tokenVersions.getOrDefault(user.getId(), 0L);
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole(), tokenVersion);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("realName", user.getRealName());
        userInfo.put("role", user.getRole());
        userInfo.put("phone", user.getPhone());
        userInfo.put("college", user.getCollege());
        userInfo.put("major", user.getMajor());
        userInfo.put("grade", user.getGrade());
        result.put("user", userInfo);

        return Result.success(result);
    }

    /** 教师自助注册（工号 + 密码 + 姓名），注册后待超管审核 */
    public Result<?> registerTeacher(String username, String password, String realName) {
        if (username == null || username.isBlank()) {
            return Result.error(400, "工号不能为空");
        }
        if (password == null || password.length() < 6) {
            return Result.error(400, "密码长度不能少于6位");
        }
        String uname = username.trim();
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, uname);
        if (userMapper.selectCount(wrapper) > 0) {
            return Result.error(400, "该工号已注册");
        }

        User user = new User();
        user.setUsername(uname);
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName == null ? "" : realName.trim());
        user.setRole("TEACHER");
        user.setStatus(1);
        user.setAuditStatus(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return Result.success("注册成功，请等待管理员审核");
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
        userInfo.put("phone", user.getPhone());
        userInfo.put("college", user.getCollege());
        userInfo.put("major", user.getMajor());
        userInfo.put("grade", user.getGrade());

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

        // 密码修改后立即使所有旧token失效
        tokenVersions.merge(userId, 1L, Long::sum);

        return Result.success();
    }

    public Result<?> bindPhone(Long userId, String phone) {
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            return Result.error(400, "手机号格式不正确");
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone);
        if (userMapper.selectCount(wrapper) > 0) {
            return Result.error(400, "该手机号已被其他账号绑定");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        user.setPhone(phone);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        return Result.success();
    }

    public Result<?> unbindPhone(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        user.setPhone(null);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        return Result.success();
    }

    public Result<?> changePhone(Long userId, String code, String newPhone) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }

        if (user.getPhone() == null || user.getPhone().isEmpty()) {
            return Result.error(400, "当前未绑定手机号，请先绑定");
        }

        // 验证原手机号的验证码
        String storedCode = codeStore.get(user.getPhone());
        Long expiry = codeExpiry.get(user.getPhone());

        if (storedCode == null || !storedCode.equals(code)) {
            return Result.error(400, "验证码错误");
        }

        if (expiry == null || System.currentTimeMillis() > expiry) {
            codeStore.remove(user.getPhone());
            codeExpiry.remove(user.getPhone());
            return Result.error(400, "验证码已过期");
        }

        // 验证新手机号格式
        if (newPhone == null || !newPhone.matches("^1[3-9]\\d{9}$")) {
            return Result.error(400, "新手机号格式不正确");
        }

        // 检查新手机号是否已被绑定
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, newPhone);
        if (userMapper.selectCount(wrapper) > 0) {
            return Result.error(400, "该手机号已被其他账号绑定");
        }

        String oldPhone = user.getPhone();
        user.setPhone(newPhone);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        // 清理旧手机号的验证码（setPhone 后 getPhone 已是新号，需用 oldPhone）
        codeStore.remove(oldPhone);
        codeExpiry.remove(oldPhone);

        return Result.success();
    }

    public Result<?> sendPhoneCode(String phone) {
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            return Result.error(400, "手机号格式不正确");
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone);
        if (userMapper.selectCount(wrapper) == 0) {
            return Result.error(404, "该手机号未绑定任何账号");
        }

        String code = String.valueOf(100000 + ThreadLocalRandom.current().nextInt(900000));
        codeStore.put(phone, code);
        codeExpiry.put(phone, System.currentTimeMillis() + 5 * 60 * 1000);

        // 已配置真实短信服务：真正发送
        if (smsService.isEnabled()) {
            boolean sent = smsService.sendVerifyCode(phone, code);
            if (!sent) {
                // 发送失败，清除无效验证码
                codeStore.remove(phone);
                codeExpiry.remove(phone);
                return Result.error(500, "短信发送失败，请稍后重试");
            }
            return Result.success("验证码已发送");
        }

        // 未配置短信服务：开发模式，验证码打印到后端日志（不回显）
        logCode(phone, code);
        Map<String, Object> result = new HashMap<>();
        result.put("message", "验证码已发送（开发模式，请查看后端日志）");
        result.put("devMode", true);
        return Result.success(result);
    }

    public Result<?> resetPasswordByPhone(String phone, String code) {
        String storedCode = codeStore.get(phone);
        Long expiry = codeExpiry.get(phone);

        if (storedCode == null || !storedCode.equals(code)) {
            return Result.error(400, "验证码错误");
        }

        if (expiry == null || System.currentTimeMillis() > expiry) {
            codeStore.remove(phone);
            codeExpiry.remove(phone);
            return Result.error(400, "验证码已过期");
        }

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getPhone, phone);
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            return Result.error(404, "该手机号未绑定任何账号");
        }

        String username = user.getUsername();
        String newPassword = username.length() >= 6
                ? username.substring(username.length() - 6)
                : String.format("%6s", username).replace(' ', '0');
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        // 密码重置后立即使所有旧token失效
        tokenVersions.merge(user.getId(), 1L, Long::sum);

        codeStore.remove(phone);
        codeExpiry.remove(phone);

        // 不再回显新密码，提示用户使用默认密码（学号后6位）登录
        return Result.success("密码已重置为默认密码，请使用学号后6位登录");
    }

    /**
     * 检查token是否仍有效（token version需匹配）
     */
    public boolean isTokenVersionValid(Long userId, long tokenVersion) {
        long currentVersion = tokenVersions.getOrDefault(userId, 0L);
        return tokenVersion >= currentVersion;
    }

    private void logCode(String phone, String code) {
        // 用 info 级别：开发模式（未接真实短信）时，生产日志级别 info 也能看到验证码
        log.info("验证码已生成（开发模式）: phone={}, code={}", phone, code);
    }
}
