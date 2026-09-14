package com.xidian.osqa.controller;

import com.xidian.osqa.common.Result;
import com.xidian.osqa.service.AdminNotifyService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 管理员通知邮箱管理接口（仅超级管理员）。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminNotifyController {

    private final AdminNotifyService adminNotifyService;

    public AdminNotifyController(AdminNotifyService adminNotifyService) {
        this.adminNotifyService = adminNotifyService;
    }

    /** 获取管理员通知邮箱列表 */
    @GetMapping("/notify-emails")
    public Result<?> getNotifyEmails() {
        return Result.success(adminNotifyService.getNotifyEmails());
    }

    /** 设置管理员通知邮箱列表（最多 10 个） */
    @PutMapping("/notify-emails")
    public Result<?> setNotifyEmails(@RequestBody Map<String, List<String>> body) {
        try {
            adminNotifyService.setNotifyEmails(body.get("emails"));
            return Result.success();
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }
}
