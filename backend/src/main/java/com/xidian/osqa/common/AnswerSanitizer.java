package com.xidian.osqa.common;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * AI 回答文本净化器（兜底过滤）。
 * <p>
 * 借鉴 DeepSeek 对推理模型输出的处理：优先剥离 &lt;think&gt;...&lt;/think&gt; 思考块，
 * 再按段落删除违禁关键词开头的段落、推导/验算分析块、主观推测/自我提问语句，
 * 返回净化后的干净教学成品文本。
 */
public final class AnswerSanitizer {

    private AnswerSanitizer() {}

    /** 剥离 <think>...</think> 思考块（含未闭合的 <think> 到文末） */
    private static final Pattern THINK_BLOCK = Pattern.compile(
            "<think>[\\s\\S]*?</think>", Pattern.CASE_INSENSITIVE);
    private static final Pattern THINK_OPEN = Pattern.compile(
            "<think>[\\s\\S]*", Pattern.CASE_INSENSITIVE);

    /** 违禁开头关键词：段落以此开头则整段删除 */
    private static final String[] FORBIDDEN_PREFIXES = {
            "重新梳理", "注意", "片段给出", "可是根据", "可能", "笔误",
            "等等", "等一下", "让我", "我需要", "我们来", "首先我", "接下来我",
            "这里我", "刚才", "不对", "抱歉", "嗯", "呃"
    };

    /** 推导/验算/草稿类段落标识词 */
    private static final String[] DRAFT_KEYWORDS = {
            "推导", "验算", "草稿", "计算一下", "算一下", "我们算",
            "验证一下", "检查一下", "排查", "纠错", "修正一下",
            "重新计算", "重新算", "推导过程", "演算"
    };

    /** 主观推测/自我提问句式（整行删除） */
    private static final Pattern[] SUBJECTIVE_PATTERNS = {
            Pattern.compile("^(如果|假设|假如|不妨假设|或许|应该|估计|猜测|我猜).{0,40}(呢|吗|？|\\?)$"),
            Pattern.compile("^(为什么|怎么会|怎么|为何).{0,40}(呢|吗|？|\\?)$"),
            Pattern.compile("^(那|那么|这).{0,20}(是不是|对不对|行不行).*$"),
            Pattern.compile("^(让我|我来|我需要|我们先|接下来).{0,30}(看看|想想|分析|检查|验证).*$")
    };

    /**
     * 净化 AI 回答文本。
     *
     * @param raw AI 原始返回
     * @return 净化后的干净文本
     */
    public static String sanitize(String raw) {
        if (raw == null || raw.isBlank()) return "";

        String text = raw;
        // 1. 剥离 <think>...</think> 思考块（DeepSeek-R1 等推理模型）
        text = THINK_BLOCK.matcher(text).replaceAll("");
        text = THINK_OPEN.matcher(text).replaceAll("");

        // 2. 按空行分段处理
        String[] paragraphs = text.split("\\n\\s*\\n");
        List<String> kept = new ArrayList<>(paragraphs.length);
        for (String para : paragraphs) {
            String trimmed = para.strip();
            if (trimmed.isEmpty()) continue;
            if (shouldDrop(trimmed)) continue;
            kept.add(para);
        }

        String result = String.join("\n\n", kept);
        // 3. 清理单行主观推测/自我提问语句
        result = cleanSubjectiveLines(result);
        // 4. 压缩多余空行
        result = result.replaceAll("\\n{3,}", "\n\n").strip();
        return result;
    }

    /** 判断一个段落是否应被删除 */
    private static boolean shouldDrop(String para) {
        // 2a. 违禁关键词开头
        for (String prefix : FORBIDDEN_PREFIXES) {
            if (para.startsWith(prefix)) return true;
        }
        // 2b. 推导/验算/草稿类段落（短段落且包含标识词）
        if (para.length() < 200) {
            for (String kw : DRAFT_KEYWORDS) {
                if (para.contains(kw)) return true;
            }
        }
        // 2c. 纯疑问句段落（整段只是一个问句）
        if (para.length() < 60 && (para.endsWith("？") || para.endsWith("?"))) {
            long comma = para.chars().filter(c -> c == '，').count();
            if (comma <= 1) return true;
        }
        return false;
    }

    /** 清理单行主观推测/自我提问语句 */
    private static String cleanSubjectiveLines(String text) {
        String[] lines = text.split("\\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.strip();
            boolean drop = false;
            for (Pattern p : SUBJECTIVE_PATTERNS) {
                if (p.matcher(trimmed).matches()) {
                    drop = true;
                    break;
                }
            }
            if (!drop) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(line);
            }
        }
        return sb.toString();
    }
}
