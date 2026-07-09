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

    /** 返回前端品牌化所需的公开设置（站点名/课程名/学校名） */
    public Map<String, String> getPublic() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("site_name", getOrDefault("site_name", "智能答疑助手"));
        map.put("course_name", getOrDefault("course_name", "本课程"));
        map.put("school_name", getOrDefault("school_name", ""));
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
