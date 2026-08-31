package com.xidian.osqa.controller;

import com.xidian.osqa.common.NetworkUtil;
import com.xidian.osqa.common.Result;
import com.xidian.osqa.security.RateLimiter;
import com.xidian.osqa.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RateLimiter rateLimiter;

    public AuthController(AuthService authService, RateLimiter rateLimiter) {
        this.authService = authService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, String> body, HttpServletRequest request) {
        // 登录限流
        String clientIp = NetworkUtil.getClientIp(request);
        if (!rateLimiter.allowLoginRequest(clientIp)) {
            return Result.error(429, "登录请求过于频繁，请稍后再试");
        }
        String username = body.get("username");
        String password = body.get("password");
        return authService.login(username, password);
    }

    @PostMapping("/register")
    public Result<?> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String realName = body.get("realName");
        return authService.registerTeacher(username, password, realName);
    }

    @PostMapping("/logout")
    public Result<?> logout() {
        return Result.success();
    }

    @GetMapping("/info")
    public Result<?> getUserInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return authService.getUserInfo(userId);
    }

    @PutMapping("/password")
    public Result<?> changePassword(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        return authService.changePassword(userId, oldPassword, newPassword);
    }

    @PostMapping("/bind-phone")
    public Result<?> bindPhone(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        String phone = body.get("phone");
        return authService.bindPhone(userId, phone);
    }

    @PostMapping("/unbind-phone")
    public Result<?> unbindPhone(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return authService.unbindPhone(userId);
    }

    @PostMapping("/change-phone")
    public Result<?> changePhone(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        String code = body.get("code");
        String newPhone = body.get("newPhone");
        return authService.changePhone(userId, code, newPhone);
    }

    @PostMapping("/send-phone-code")
    public Result<?> sendPhoneCode(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        return authService.sendPhoneCode(phone);
    }

    @PostMapping("/reset-password-by-phone")
    public Result<?> resetPasswordByPhone(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String code = body.get("code");
        return authService.resetPasswordByPhone(phone, code);
    }
}
