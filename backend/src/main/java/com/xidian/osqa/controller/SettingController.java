package com.xidian.osqa.controller;

import com.xidian.osqa.common.Result;
import com.xidian.osqa.service.SystemSettingService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class SettingController {

    private final SystemSettingService settingService;

    public SettingController(SystemSettingService settingService) {
        this.settingService = settingService;
    }

    /** 公开接口：登录页等未登录场景获取品牌化信息 */
    @GetMapping("/settings/public")
    public Result<?> getPublic() {
        return Result.success(settingService.getPublic());
    }

    @GetMapping("/admin/settings")
    public Result<?> getAll() {
        return Result.success(settingService.getAll());
    }

    @PutMapping("/admin/settings")
    public Result<?> update(@RequestBody Map<String, String> body) {
        settingService.setAll(body);
        return Result.success();
    }
}
