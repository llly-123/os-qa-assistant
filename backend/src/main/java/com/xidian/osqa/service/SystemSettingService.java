package com.xidian.osqa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xidian.osqa.entity.SystemSetting;
import com.xidian.osqa.mapper.SystemSettingMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SystemSettingService {

    private final SystemSettingMapper settingMapper;

    public SystemSettingService(SystemSettingMapper settingMapper) {
        this.settingMapper = settingMapper;
    }

    public String get(String key) {
        SystemSetting s = settingMapper.selectById(key);
        return s != null ? s.getSettingValue() : null;
    }

    public String getOrDefault(String key, String defaultValue) {
        String v = get(key);
        return (v == null || v.isBlank()) ? defaultValue : v;
    }

    public Map<String, String> getAll() {
        List<SystemSetting> all = settingMapper.selectList(null);
        Map<String, String> map = new LinkedHashMap<>();
        for (SystemSetting s : all) {
            map.put(s.getSettingKey(), s.getSettingValue() != null ? s.getSettingValue() : "");
        }
        return map;
    }

    public void set(String key, String value) {
        SystemSetting s = settingMapper.selectById(key);
        if (s == null) {
            s = new SystemSetting();
            s.setSettingKey(key);
            s.setSettingValue(value);
            s.setUpdateTime(LocalDateTime.now());
            settingMapper.insert(s);
        } else {
            s.setSettingValue(value);
            s.setUpdateTime(LocalDateTime.now());
            settingMapper.updateById(s);
        }
    }

    public void setAll(Map<String, String> entries) {
        if (entries == null) return;
        for (Map.Entry<String, String> e : entries.entrySet()) {
            set(e.getKey(), e.getValue());
        }
    }
}
