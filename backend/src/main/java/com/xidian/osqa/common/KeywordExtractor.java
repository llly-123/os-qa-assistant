package com.xidian.osqa.common;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.SegToken;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 通用中文/英文关键词频次提取（课程无关，替代原先硬编码的操作系统关键词表）。
 *
 * 使用 jieba 中文分词对文本切词，过滤停用词/标点/数字/单字后按文本去重统计词频，
 * 取频次最高的若干词。替代原先基于 2~4 字 n-gram 滑窗的实现——后者没有真正分词，
 * 会把 "判断晶体管" 切成 "判断晶体 / 断晶体 / 晶体管" 等一堆重叠碎片，且抑制逻辑
 * 无法合并相邻不互含的片段（如 "负反馈的 / 反馈的组 / 馈的组态"）。
 *
 * 策略：JiebaSegmenter(INDEX 模式，倾向保留长词) 切词 → 每条文本内去重 → 跨文本累加
 * 频次 → 按频次降序（同频按字典序）取前 N。
 */
public final class KeywordExtractor {

    private KeywordExtractor() {}

    /** JiebaSegmenter 读取全局词典，process() 无状态，可作为单例被多线程共享 */
    private static final JiebaSegmenter SEGMENTER = new JiebaSegmenter();

    /** 兜底过滤：token 至少含一个中文或英文字母才视为候选词（排除纯数字/标点/符号） */
    private static final Pattern WORD_PATTERN = Pattern.compile(".*[\\u4e00-\\u9fa5a-zA-Z].*");

    private static final Set<String> STOP_WORDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一个", "上", "也", "很",
            "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好", "自己", "这", "他", "她", "它",
            "们", "那", "些", "什么", "怎么", "如何", "为什么", "吗", "呢", "吧", "啊", "呀", "哦", "嗯",
            "能", "可以", "应该", "还是", "或者", "但", "但是", "而", "而且", "如果", "因为", "所以",
            "虽然", "不过", "只是", "关于", "对于", "请问", "一下", "这个", "那个", "里面", "外面",
            "时候", "现在", "下面", "上面", "前面", "后面", "以及", "并且", "还有", "不是", "不能",
            "不要", "没法", "无法", "一下", "哪些", "哪种", "几个", "多个", "这样", "那样", "一样",
            "进行", "通过", "根据", "按照", "属于", "处于", "称为", "叫做", "比如", "例如", "如下",
            "什么是", "什么叫", "区别", "原理", "概念", "作用", "特点", "应用", "举例", "相关",
            // 提问/说明类动词（非知识点）
            "判断", "说明", "解释", "分析", "比较", "描述", "给出", "列出", "简述", "简答", "论述",
            "计算", "推导", "证明", "画出", "讨论", "介绍", "定义", "阐述", "列举", "回答", "求解",
            "试问", "叙述", "概述", "总结", "归纳", "了解", "理解", "掌握", "熟悉", "知道",
            // 抽象/泛化名词：独立出现时几乎不是知识点；其长词形式（如"进程状态""存储结构"）不受影响
            "状态", "情况", "问题", "方面", "内容", "部分", "形式", "方式", "过程", "结果", "原因",
            "影响", "关系", "条件", "因素", "现象", "表现", "规律", "观点", "看法",
            // 泛动词
            "工作", "运行", "存在", "发生", "产生", "形成", "实现", "完成", "具有", "包括",
            "使用", "利用", "采用", "需要", "要求", "导致", "引起", "达到", "保持", "改变", "变化",
            "得到", "获得",
            "the", "is", "are", "what", "how", "why", "and", "of", "to", "in", "for"
    )));

    /**
     * 从一批文本中提取高频关键词。
     * @param texts 文本列表
     * @param limit 返回条数
     * @return 按频次降序排列的关键词列表，每项含 word/count
     */
    public static List<Map<String, Object>> extract(List<String> texts, int limit) {
        if (texts == null || texts.isEmpty()) return Collections.emptyList();

        Map<String, Integer> count = new HashMap<>();
        for (String text : texts) {
            if (text == null || text.isBlank()) continue;
            // 每个文本内对词去重，避免单条长文本刷高某个词
            Set<String> seen = new HashSet<>();
            List<SegToken> tokens = SEGMENTER.process(text, JiebaSegmenter.SegMode.INDEX);
            for (SegToken t : tokens) {
                String w = t.word == null ? "" : t.word.trim();
                if (w.isEmpty()) continue;
                if (STOP_WORDS.contains(w)) continue;
                if (!WORD_PATTERN.matcher(w).matches()) continue; // 纯数字/标点/符号
                if (w.length() < 2) continue;                      // 单字/单字母噪声
                if (w.matches("[A-Za-z]+")) {                      // 英文统一小写，便于聚合
                    w = w.toLowerCase();
                }
                seen.add(w);
            }
            for (String w : seen) {
                count.merge(w, 1, Integer::sum);
            }
        }

        // 抑制子串冗余：jieba INDEX 模式会同时输出长词与构成它的子词
        // （如 "晶体"/"晶体管"、"进程"/"同步"/"进程同步"）。若短词是某频次不低于
        // 它的长词的子串，则剔除短词，保留更长、信息量更大的词。
        List<String> words = new ArrayList<>(count.keySet());
        Map<String, Integer> filtered = new LinkedHashMap<>();
        for (String w : words) {
            boolean suppressed = false;
            for (String other : words) {
                if (other.length() > w.length() && other.contains(w) && count.get(other) >= count.get(w)) {
                    suppressed = true;
                    break;
                }
            }
            if (!suppressed) {
                filtered.put(w, count.get(w));
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        filtered.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(limit)
                .forEach(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("word", e.getKey());
                    m.put("count", e.getValue());
                    result.add(m);
                });
        return result;
    }
}
