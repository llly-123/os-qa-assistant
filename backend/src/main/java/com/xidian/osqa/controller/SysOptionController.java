package com.xidian.osqa.controller;

import com.xidian.osqa.common.Result;
import com.xidian.osqa.entity.SysOption;
import com.xidian.osqa.service.SysOptionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/options")
public class SysOptionController {

    private final SysOptionService optionService;

    public SysOptionController(SysOptionService optionService) {
        this.optionService = optionService;
    }

    @GetMapping
    public Result<?> getAllGrouped() {
        Map<String, List<String>> grouped = optionService.getAllGrouped();
        return Result.success(grouped);
    }

    @GetMapping("/{category}")
    public Result<?> getByCategory(@PathVariable String category) {
        List<SysOption> options = optionService.getByCategory(category);
        return Result.success(options);
    }

    @PostMapping
    public Result<?> addOption(@RequestBody Map<String, String> body) {
        String category = body.get("category");
        String value = body.get("value");
        if (category == null || value == null || category.isBlank() || value.isBlank()) {
            return Result.error(400, "分类和值不能为空");
        }
        SysOption option = optionService.addOption(category, value);
        return Result.success(option);
    }

    @PutMapping("/{id}")
    public Result<?> updateOption(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String value = body.get("value");
        if (value == null || value.isBlank()) {
            return Result.error(400, "值不能为空");
        }
        optionService.updateOption(id, value);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteOption(@PathVariable Long id) {
        optionService.deleteOption(id);
        return Result.success();
    }
}
