package com.xidian.osqa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xidian.osqa.entity.Knowledge;
import com.xidian.osqa.entity.KnowledgeChunk;
import com.xidian.osqa.mapper.KnowledgeChunkMapper;
import com.xidian.osqa.mapper.KnowledgeMapper;
import jakarta.annotation.PostConstruct;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class KnowledgeEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeEmbeddingService.class);

    private static final Set<String> STOP_WORDS = Set.of(
            "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个",
            "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好",
            "自己", "这", "他", "她", "它", "们", "那", "些", "什么", "怎么", "如何", "为什么",
            "吗", "呢", "吧", "啊", "呀", "哦", "嗯", "能", "可以", "应该", "还是", "或者",
            "但", "但是", "而", "而且", "如果", "因为", "所以", "虽然", "不过", "只是",
            "the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would", "could",
            "should", "may", "might", "shall", "can", "need", "dare", "ought",
            "used", "to", "of", "in", "for", "on", "with", "at", "by", "from",
            "as", "into", "through", "during", "before", "after", "above", "below",
            "between", "out", "off", "over", "under", "again", "further", "then",
            "once", "and", "but", "or", "nor", "not", "so", "if", "than", "too",
            "very", "just", "about", "up", "down", "all", "each", "both", "few",
            "more", "most", "other", "some", "such", "no", "only", "same"
    );

    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]+");
    private static final Pattern ENGLISH_PATTERN = Pattern.compile("[a-zA-Z]+");

    private final KnowledgeChunkMapper chunkMapper;
    private final KnowledgeMapper knowledgeMapper;

    @Value("${knowledge.chunk-size:500}")
    private int chunkSize;

    @Value("${knowledge.chunk-overlap:50}")
    private int chunkOverlap;

    @Value("${knowledge.upload-dir:./uploads/knowledge}")
    private String uploadDir;

    private static final int VECTOR_DIM = 256;

    private final Map<Long, double[]> chunkVectors = new HashMap<>();
    private final Map<String, Double> idfCache = new HashMap<>();
    private volatile List<KnowledgeChunk> cachedChunks = new ArrayList<>();
    private volatile boolean indexReady = false;

    public KnowledgeEmbeddingService(KnowledgeChunkMapper chunkMapper, KnowledgeMapper knowledgeMapper) {
        this.chunkMapper = chunkMapper;
        this.knowledgeMapper = knowledgeMapper;
    }

    @PostConstruct
    public void init() {
        loadExistingChunks();
    }

    private void loadExistingChunks() {
        try {
            List<KnowledgeChunk> allChunks = chunkMapper.selectList(null);
            if (allChunks.isEmpty()) {
                log.info("知识库为空，无需加载索引");
                indexReady = true;
                return;
            }

            log.info("开始加载 {} 个知识块并构建检索索引...", allChunks.size());
            buildIndex(allChunks);
            indexReady = true;
            log.info("检索索引构建完成，共 {} 个知识块", allChunks.size());
        } catch (Exception e) {
            log.error("加载知识块失败: {}", e.getMessage());
            indexReady = true;
        }
    }

    private void buildIndex(List<KnowledgeChunk> chunks) {
        this.cachedChunks = new ArrayList<>(chunks);
        Map<String, Integer> docFreq = new HashMap<>();
        int totalDocs = chunks.size();

        List<List<String>> allTokenLists = new ArrayList<>();
        for (KnowledgeChunk chunk : chunks) {
            List<String> tokens = tokenize(chunk.getContent());
            allTokenLists.add(tokens);
            Set<String> uniqueTokens = new HashSet<>(tokens);
            for (String token : uniqueTokens) {
                docFreq.merge(token, 1, Integer::sum);
            }
        }

        idfCache.clear();
        for (Map.Entry<String, Integer> entry : docFreq.entrySet()) {
            idfCache.put(entry.getKey(), Math.log((double) totalDocs / (entry.getValue() + 1)));
        }

        chunkVectors.clear();
        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Double> tfidf = computeTfIdf(allTokenLists.get(i));
            chunkVectors.put(chunks.get(i).getId(), sparseToVector(tfidf));
        }

        log.info("TF-IDF索引构建完成: {} 个词项, {} 个文档向量", idfCache.size(), chunkVectors.size());
    }

    public void processDocument(Long knowledgeId, String filePath) {
        log.info("开始处理文档: knowledgeId={}, filePath={}", knowledgeId, filePath);

        File file = new File(filePath);
        if (!file.exists() || file.length() == 0) {
            log.error("文件不存在或为空: {}, size={}", filePath, file.exists() ? file.length() : -1);
            markFailed(knowledgeId, "文件不存在或为空");
            return;
        }

        String text = parseDocument(filePath);
        if (text == null || text.isBlank()) {
            log.warn("文档解析结果为空(可能是扫描版PDF): {}", filePath);
            markFailed(knowledgeId, "文档解析结果为空，可能是扫描版PDF，需要OCR支持");
            return;
        }

        log.info("文档解析成功，文本长度: {} 字符", text.length());

        List<String> chunks = splitText(text);
        log.info("文档切分为 {} 个知识块", chunks.size());

        List<KnowledgeChunk> savedChunks = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setKnowledgeId(knowledgeId);
            chunk.setContent(chunks.get(i));
            chunk.setChunkIndex(i);
            chunk.setSourceFile(file.getName());
            chunk.setCreateTime(LocalDateTime.now());
            chunkMapper.insert(chunk);
            savedChunks.add(chunk);
        }

        rebuildIndex();

        Knowledge knowledge = knowledgeMapper.selectById(knowledgeId);
        if (knowledge != null) {
            knowledge.setChunkCount(savedChunks.size());
            knowledge.setStatus(1);
            knowledgeMapper.updateById(knowledge);
        }

        log.info("文档处理完成: {} 个知识块已入库并索引", savedChunks.size());
    }

    public void processText(Long knowledgeId, String title, String content) {
        log.info("开始处理文本导入: knowledgeId={}, title={}, 内容长度={}", knowledgeId, title, content.length());

        if (content == null || content.isBlank()) {
            markFailed(knowledgeId, "文本内容为空");
            return;
        }

        List<String> chunks = splitText(content);
        log.info("文本切分为 {} 个知识块", chunks.size());

        List<KnowledgeChunk> savedChunks = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setKnowledgeId(knowledgeId);
            chunk.setContent(chunks.get(i));
            chunk.setChunkIndex(i);
            chunk.setSourceFile(title);
            chunk.setCreateTime(LocalDateTime.now());
            chunkMapper.insert(chunk);
            savedChunks.add(chunk);
        }

        rebuildIndex();

        Knowledge knowledge = knowledgeMapper.selectById(knowledgeId);
        if (knowledge != null) {
            knowledge.setChunkCount(savedChunks.size());
            knowledge.setStatus(1);
            knowledgeMapper.updateById(knowledge);
        }

        log.info("文本导入处理完成: {} 个知识块已入库并索引", savedChunks.size());
    }

    String parseDocument(String filePath) {
        try {
            String ext = filePath.substring(filePath.lastIndexOf('.') + 1).toLowerCase();
            return switch (ext) {
                case "pdf" -> parsePdf(filePath);
                case "pptx", "ppt" -> parsePptx(filePath);
                case "docx", "doc" -> parseDocx(filePath);
                case "txt" -> parseTxt(filePath);
                default -> {
                    log.warn("不支持的文件格式: {}", ext);
                    yield null;
                }
            };
        } catch (Exception e) {
            log.error("解析文档失败: {}", filePath, e);
            return null;
        }
    }

    private void markFailed(Long knowledgeId, String reason) {
        try {
            Knowledge k = knowledgeMapper.selectById(knowledgeId);
            if (k != null) {
                k.setStatus(2);
                knowledgeMapper.updateById(k);
                log.info("已标记知识记录为失败: id={}, 原因={}", knowledgeId, reason);
            }
        } catch (Exception e) {
            log.error("更新知识记录状态失败: id={}", knowledgeId, e);
        }
    }

    private String parsePdf(String filePath) throws Exception {
        File file = new File(filePath);
        log.info("开始解析PDF: {}, 大小: {}MB", file.getName(), file.length() / 1024 / 1024);
        
        try (PDDocument doc = Loader.loadPDF(file)) {
            int totalPages = doc.getNumberOfPages();
            log.info("PDF共 {} 页", totalPages);
            
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(doc);
            
            double charsPerPage = totalPages > 0 ? (double) text.length() / totalPages : 0;
            log.info("提取文本长度: {}, 每页平均: {} 字符", text.length(), (int) charsPerPage);
            
            if (charsPerPage < 10) {
                log.info("检测到扫描版PDF(每页平均仅{}字符)，尝试OCR识别...", (int) charsPerPage);
                text = ocrPdf(doc, totalPages);
                if (text.isBlank()) {
                    throw new RuntimeException("OCR识别结果为空。请使用「文本导入」功能，将教材内容粘贴或上传TXT文件。");
                }
                log.info("OCR识别完成，文本长度: {} 字符", text.length());
            }
            
            return text;
        }
    }

    private String ocrPdf(PDDocument doc, int totalPages) {
        try {
            String tessdataPath = findTessdataPath();
            if (tessdataPath == null) {
                log.warn("未找到Tesseract tessdata目录，OCR不可用。请安装Tesseract或使用文本导入功能");
                throw new RuntimeException("服务器未安装OCR引擎，请使用「文本导入」功能");
            }

            net.sourceforge.tess4j.Tesseract tesseract = new net.sourceforge.tess4j.Tesseract();
            tesseract.setDatapath(tessdataPath);
            tesseract.setLanguage("chi_sim+eng");
            tesseract.setPageSegMode(3);

            org.apache.pdfbox.rendering.PDFRenderer renderer = new org.apache.pdfbox.rendering.PDFRenderer(doc);
            StringBuilder allText = new StringBuilder();

            for (int p = 0; p < totalPages; p++) {
                try {
                    java.awt.image.BufferedImage pageImage = renderer.renderImageWithDPI(p, 72);
                    String pageText = tesseract.doOCR(pageImage);
                    allText.append(pageText).append("\n");
                    if ((p + 1) % 10 == 0 || p == totalPages - 1) {
                        log.info("OCR进度: {}/{} 页, 累计文本长度: {}", p + 1, totalPages, allText.length());
                    }
                } catch (Exception e) {
                    log.warn("OCR第 {} 页失败: {}", p + 1, e.getMessage());
                }
            }

            String result = allText.toString().trim();
            if (result.isEmpty()) {
                throw new RuntimeException("OCR识别结果为空，请检查语言包是否完整或PDF是否为图片。建议使用「文本导入」功能。");
            }
            return result;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("OCR处理失败", e);
            throw new RuntimeException("OCR处理失败: " + e.getMessage() + "。请使用「文本导入」功能");
        }
    }

    private String findTessdataPath() {
        String[] candidates = {
            System.getenv("TESSDATA_PREFIX"),
            System.getProperty("user.dir") + File.separator + "tessdata",
            "C:\\Program Files\\Tesseract-OCR\\tessdata",
            "/usr/share/tesseract-ocr/5/tessdata",
            "/usr/share/tesseract-ocr/4.00/tessdata",
            "/usr/share/tessdata"
        };

        for (String path : candidates) {
            if (path == null) continue;
            File dir = new File(path);
            if (dir.isDirectory()) {
                File chiSim = new File(dir, "chi_sim.traineddata");
                if (chiSim.exists()) {
                    log.info("找到tessdata目录: {}", path);
                    return path;
                }
                File eng = new File(dir, "eng.traineddata");
                if (eng.exists()) {
                    log.info("找到tessdata目录(仅英文): {}", path);
                    return path;
                }
            }
        }

        log.warn("未找到包含中文语言包的tessdata目录");
        return null;
    }

    private String parsePptx(String filePath) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(filePath);
             XMLSlideShow ppt = new XMLSlideShow(fis)) {
            for (XSLFSlide slide : ppt.getSlides()) {
                sb.append(slide.getTitle() != null ? slide.getTitle() : "").append("\n");
                for (var shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        String text = textShape.getText();
                        if (text != null && !text.isBlank()) {
                            sb.append(text).append("\n");
                        }
                    }
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private String parseDocx(String filePath) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(filePath);
             XWPFDocument doc = new XWPFDocument(fis)) {
            for (XWPFParagraph para : doc.getParagraphs()) {
                String text = para.getText();
                if (text != null && !text.isBlank()) {
                    sb.append(text).append("\n");
                }
            }
        }
        return sb.toString();
    }

    private String parseTxt(String filePath) throws Exception {
        return Files.readString(new File(filePath).toPath(), StandardCharsets.UTF_8);
    }

    List<String> splitText(String text) {
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\\n\\s*\\n");

        StringBuilder currentChunk = new StringBuilder();
        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) continue;

            if (currentChunk.length() + trimmed.length() > chunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                String overlapText = getOverlapText(currentChunk.toString());
                currentChunk = new StringBuilder(overlapText);
            }

            if (trimmed.length() > chunkSize) {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                }
                List<String> subChunks = splitLongParagraph(trimmed);
                chunks.addAll(subChunks.subList(0, subChunks.size() - 1));
                currentChunk = new StringBuilder(subChunks.get(subChunks.size() - 1));
            } else {
                currentChunk.append(trimmed).append("\n\n");
            }
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    private List<String> splitLongParagraph(String text) {
        List<String> chunks = new ArrayList<>();
        String[] sentences = text.split("(?<=[。！？；.!?;])");
        StringBuilder current = new StringBuilder();

        for (String sentence : sentences) {
            if (current.length() + sentence.length() > chunkSize && current.length() > 0) {
                chunks.add(current.toString().trim());
                String overlap = current.length() > chunkOverlap
                        ? current.substring(current.length() - chunkOverlap)
                        : current.toString();
                current = new StringBuilder(overlap);
            }
            current.append(sentence);
        }

        if (current.length() > 0) {
            chunks.add(current.toString().trim());
        }

        if (chunks.isEmpty()) {
            for (int i = 0; i < text.length(); i += chunkSize - chunkOverlap) {
                int end = Math.min(i + chunkSize, text.length());
                chunks.add(text.substring(i, end));
                if (end == text.length()) break;
            }
        }

        return chunks;
    }

    private String getOverlapText(String text) {
        if (text.length() <= chunkOverlap) return text;
        return text.substring(text.length() - chunkOverlap);
    }

    public List<String> retrieve(String query, int maxResults) {
        List<KnowledgeChunk> allChunks = this.cachedChunks;
        if (allChunks.isEmpty()) {
            log.info("知识库为空，无检索结果");
            return Collections.emptyList();
        }

        List<String> queryTokens = tokenize(query);
        log.info("查询分词结果: query='{}', tokens={}", query, queryTokens);
        if (queryTokens.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map.Entry<KnowledgeChunk, Double>> scored = new ArrayList<>();
        for (KnowledgeChunk chunk : allChunks) {
            double kwScore = keywordScore(queryTokens, chunk.getContent());
            log.debug("知识块 {} 关键词得分: {}", chunk.getId(), kwScore);

            double tfidfScore = 0;
            double[] chunkVector = chunkVectors.get(chunk.getId());
            if (chunkVector != null) {
                Map<String, Double> queryTfIdf = computeTfIdf(queryTokens);
                double[] queryVector = sparseToVector(queryTfIdf);
                tfidfScore = cosineSimilarity(queryVector, chunkVector);
            }

            double finalScore = kwScore * 0.7 + tfidfScore * 0.3;

            if (finalScore > 0) {
                scored.add(Map.entry(chunk, finalScore));
            }
        }

        scored.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        List<String> results = new ArrayList<>();
        for (int i = 0; i < Math.min(maxResults, scored.size()); i++) {
            KnowledgeChunk chunk = scored.get(i).getKey();
            String content = chunk.getContent();
            if (chunk.getSourceFile() != null) {
                content += "\n[来源: " + chunk.getSourceFile() + "]";
            }
            results.add(content);
            log.debug("检索结果: score={}, source={}", scored.get(i).getValue(), chunk.getSourceFile());
        }

        log.info("检索完成: query='{}', 结果数={}", query.substring(0, Math.min(query.length(), 20)), results.size());
        return results;
    }

    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();

        var chineseMatcher = CHINESE_PATTERN.matcher(text);
        while (chineseMatcher.find()) {
            String segment = chineseMatcher.group();
            for (int n = 4; n >= 2; n--) {
                for (int i = 0; i <= segment.length() - n; i++) {
                    String ngram = segment.substring(i, i + n);
                    if (!STOP_WORDS.contains(ngram)) {
                        tokens.add(ngram);
                    }
                }
            }
            for (char c : segment.toCharArray()) {
                String ch = String.valueOf(c);
                if (!STOP_WORDS.contains(ch)) {
                    tokens.add(ch);
                }
            }
        }

        var englishMatcher = ENGLISH_PATTERN.matcher(text);
        while (englishMatcher.find()) {
            String word = englishMatcher.group().toLowerCase();
            if (!STOP_WORDS.contains(word) && word.length() > 1) {
                tokens.add(word);
            }
        }

        return tokens;
    }

    private Map<String, Double> computeTfIdf(List<String> tokens) {
        Map<String, Double> tf = new HashMap<>();
        for (String token : tokens) {
            tf.merge(token, 1.0, Double::sum);
        }
        int totalTokens = tokens.size();
        if (totalTokens == 0) return Collections.emptyMap();

        Map<String, Double> tfidf = new HashMap<>();
        for (Map.Entry<String, Double> entry : tf.entrySet()) {
            double tfVal = entry.getValue() / totalTokens;
            double idfVal = idfCache.getOrDefault(entry.getKey(), Math.log(100.0));
            tfidf.put(entry.getKey(), tfVal * idfVal);
        }
        return tfidf;
    }

    private double[] sparseToVector(Map<String, Double> tfidf) {
        double[] vector = new double[VECTOR_DIM];
        for (Map.Entry<String, Double> entry : tfidf.entrySet()) {
            int hash = Math.abs(entry.getKey().hashCode()) % VECTOR_DIM;
            vector[hash] += entry.getValue();
        }
        // L2 normalize
        double norm = 0;
        for (double v : vector) {
            norm += v * v;
        }
        if (norm > 0) {
            norm = Math.sqrt(norm);
            for (int i = 0; i < VECTOR_DIM; i++) {
                vector[i] /= norm;
            }
        }
        return vector;
    }

    private double cosineSimilarity(double[] a, double[] b) {
        double dotProduct = 0, normA = 0, normB = 0;
        for (int i = 0; i < VECTOR_DIM; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private double keywordScore(List<String> queryTokens, String content) {
        String lowerContent = content.toLowerCase();
        double score = 0;
        for (String token : queryTokens) {
            int idx = 0;
            int count = 0;
            while ((idx = lowerContent.indexOf(token.toLowerCase(), idx)) != -1) {
                count++;
                idx += token.length();
            }
            if (count > 0) {
                score += count * token.length();
            }
        }
        return score;
    }

    public void removeDocument(Long knowledgeId) {
        LambdaQueryWrapper<KnowledgeChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeChunk::getKnowledgeId, knowledgeId);
        chunkMapper.delete(wrapper);

        rebuildIndex();
        log.info("已删除文档 {} 的所有知识块并重建索引", knowledgeId);
    }

    public void rebuildIndex() {
        log.info("开始重建检索索引...");
        List<KnowledgeChunk> allChunks = chunkMapper.selectList(null);
        if (!allChunks.isEmpty()) {
            buildIndex(allChunks);
        }
        indexReady = true;
        log.info("检索索引重建完成");
    }

    public int getChunkCount() {
        return Math.toIntExact(chunkMapper.selectCount(null));
    }
}
