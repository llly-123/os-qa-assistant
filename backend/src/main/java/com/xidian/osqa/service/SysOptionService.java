package com.xidian.osqa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xidian.osqa.entity.SysOption;
import com.xidian.osqa.mapper.SysOptionMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SysOptionService {

    private final SysOptionMapper optionMapper;

    public SysOptionService(SysOptionMapper optionMapper) {
        this.optionMapper = optionMapper;
    }

    public List<SysOption> getByCategory(String category) {
        LambdaQueryWrapper<SysOption> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysOption::getCategory, category)
               .orderByAsc(SysOption::getSortOrder);
        return optionMapper.selectList(wrapper);
    }

    public Map<String, List<String>> getAllGrouped() {
        List<SysOption> all = optionMapper.selectList(null);
        return all.stream()
                .collect(Collectors.groupingBy(
                        SysOption::getCategory,
                        Collectors.mapping(SysOption::getOptionValue, Collectors.toList())
                ));
    }

    public SysOption addOption(String category, String value) {
        long count = optionMapper.selectCount(new LambdaQueryWrapper<SysOption>()
                .eq(SysOption::getCategory, category));
        SysOption option = new SysOption();
        option.setCategory(category);
        option.setOptionValue(value);
        option.setSortOrder((int) count + 1);
        optionMapper.insert(option);
        return option;
    }

    public void deleteOption(Long id) {
        optionMapper.deleteById(id);
    }

    public void updateOption(Long id, String value) {
        SysOption option = optionMapper.selectById(id);
        if (option != null) {
            option.setOptionValue(value);
            optionMapper.updateById(option);
        }
    }
}
