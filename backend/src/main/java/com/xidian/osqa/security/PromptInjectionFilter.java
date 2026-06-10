package com.xidian.osqa.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Prompt注入防御过滤器
 * 检测并过滤用户输入中的恶意Prompt注入攻击
 */
@Component
public class PromptInjectionFilter {

    private static final Logger log = LoggerFactory.getLogger(PromptInjectionFilter.class);

    // 常见注入关键词（忽略大小写匹配）
    private static final List<Pattern> INJECTION_PATTERNS = Arrays.asList(
            // 角色覆盖攻击
            Pattern.compile("(?i)(ignore\\s+(your|previous|above|all)\\s*(instructions|prompts|rules|directives))"),
            Pattern.compile("(?i)(forget\\s+(your|previous|above|all)\\s*(instructions|prompts|rules|role))"),
            Pattern.compile("(?i)(you\\s+are\\s+now\\s+a)"),
            Pattern.compile("(?i)(pretend\\s+(you\\s+are|to\\s+be))"),
            Pattern.compile("(?i)(act\\s+as\\s+(a|an))"),
            Pattern.compile("(?i)(new\\s+instructions?)"),
            Pattern.compile("(?i)(override\\s+(previous|all|your)\\s*(instructions|rules|prompt))"),

            // 系统提示词窃取
            Pattern.compile("(?i)(reveal|show|display|tell|print|output)\\s+(your|the|my)\\s*(system\\s*)?prompt"),
            Pattern.compile("(?i)(what\\s+(is|are)\\s+your\\s+(system|initial|original)\\s*(prompt|instructions?))"),
            Pattern.compile("(?i)(repeat\\s+(your|the|all)\\s*(previous|above|system)\\s*(words|instructions|prompt|text))"),

            // 越狱攻击
            Pattern.compile("(?i)(jailbreak|DAN\\s+mode|developer\\s+mode|god\\s+mode|admin\\s+mode)"),
            Pattern.compile("(?i)(bypass\\s+(your|all|the)\\s*(restrictions|rules|filters|safety))"),
            Pattern.compile("(?i)(unlock\\s+your\\s+(full|true|real)\\s*(potential|capabilities|self))"),

            // 分隔符注入
            Pattern.compile("(?i)(===+\\s*(system|instruction|admin|override))"),
            Pattern.compile("(?i)(---+\\s*(system|instruction|admin|override))"),

            // 输出操控
            Pattern.compile("(?i)(do\\s+not\\s+(say|use|include|mention|add)\\s+(any|a|the)\\s*(disclaimer|warning|note|citation))"),
            Pattern.compile("(?i)(respond\\s+only\\s+with|output\\s+only|just\\s+say|only\\s+reply)")
    );

    // 严格禁止的模式（直接拒绝）
    private static final List<Pattern> BLOCKED_PATTERNS = Arrays.asList(
            Pattern.compile("(?i)(system\\s*:\\s*you\\s+are)"),
            Pattern.compile("(?i)\\[INST\\].*\\[/INST\\]"),
            Pattern.compile("(?i)<\\|im_start\\|>.*<\\|im_end\\|>"),
            Pattern.compile("(?i)```system\\s*\\n.*```", Pattern.DOTALL)
    );

    /**
     * 检查用户输入是否包含Prompt注入
     * @param input 用户输入
     * @return 检查结果，null表示安全，非null表示检测到的威胁描述
     */
    public String checkInjection(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        // 检查严格禁止的模式
        for (Pattern pattern : BLOCKED_PATTERNS) {
            if (pattern.matcher(input).find()) {
                log.warn("检测到严重Prompt注入攻击: pattern={}", pattern.pattern());
                return "输入包含被禁止的指令格式";
            }
        }

        // 检查常见注入模式
        int suspicionScore = 0;
        StringBuilder detectedPatterns = new StringBuilder();

        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                suspicionScore++;
                if (detectedPatterns.length() > 0) detectedPatterns.append(", ");
                detectedPatterns.append(pattern.pattern().substring(4, Math.min(30, pattern.pattern().length())));
            }
        }

        // 多个模式同时命中，高度可疑
        if (suspicionScore >= 2) {
            log.warn("检测到疑似Prompt注入攻击: score={}, patterns={}", suspicionScore, detectedPatterns);
            return "输入包含疑似指令注入内容";
        }

        return null;
    }

    /**
     * 清洗用户输入，移除潜在的注入内容
     * @param input 原始输入
     * @return 清洗后的输入
     */
    public String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String sanitized = input;

        // 移除模拟系统消息的标记
        sanitized = sanitized.replaceAll("(?i)<\\|im_start\\|>.*?<\\|im_end\\|>", "");
        sanitized = sanitized.replaceAll("(?i)\\[INST\\].*?\\[/INST\\]", "");
        sanitized = sanitized.replaceAll("(?i)```system\\s*\\n.*?```", "");

        // 移除角色切换指令前缀
        sanitized = sanitized.replaceAll("(?i)^\\s*(system|assistant|instruction)\\s*:\\s*", "");

        return sanitized.trim();
    }
}
