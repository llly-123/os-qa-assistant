package com.xidian.osqa.controller;

import com.xidian.osqa.common.Result;
import com.xidian.osqa.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        return authService.login(username, password);
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

    @PostMapping("/send-code")
    public Result<?> sendVerifyCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        return authService.sendVerifyCode(email);
    }

    @PostMapping("/reset-password")
    public Result<?> resetPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String code = body.get("code");
        return authService.resetPassword(email, code);
    }
}
