package com.xidian.osqa.service;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final ChatLanguageModel chatModel;
    private final KnowledgeEmbeddingService embeddingService;

    private static final String SYSTEM_PROMPT = """
            你是西安电子科技大学《操作系统》课程的AI答疑助教。你的职责是：
            1. 严格基于提供的教材内容回答问题，确保答案准确无误。
            2. 如果教材中没有相关内容，请明确告知学生"教材中未找到相关内容"，并建议查阅其他资料或咨询老师。
            3. 回答时尽量使用教材原文的术语和表述，保持学术严谨性。
            4. 如果提供了网络搜索结果，可以补充说明，但需明确标注来源。
            5. 不要回答与操作系统课程无关的问题。
            6. 回答格式清晰，使用Markdown格式，包含代码示例和公式时使用代码块。
            7. 在回答末尾，如果引用了教材内容，请标注参考资料来源。
            """;

    public RagService(ChatLanguageModel chatModel, KnowledgeEmbeddingService embeddingService) {
        this.chatModel = chatModel;
        this.embeddingService = embeddingService;
    }

    public String answer(String question, boolean webSearch) {
        try {
            List<String> relevantChunks = embeddingService.retrieve(question, 8);

            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new SystemMessage(SYSTEM_PROMPT));

            StringBuilder contextBuilder = new StringBuilder();
            if (!relevantChunks.isEmpty()) {
                contextBuilder.append("以下是教材中检索到的相关内容：\n\n");
                for (int i = 0; i < relevantChunks.size(); i++) {
                    contextBuilder.append("【片段").append(i + 1).append("】\n")
                            .append(relevantChunks.get(i))
                            .append("\n\n");
                }
                contextBuilder.append("请基于以上教材内容回答学生的问题。如果以上内容不足以回答问题，请如实说明。\n\n");
            } else {
                contextBuilder.append("注意：知识库中未检索到与问题直接相关的教材内容。");
                if (!webSearch) {
                    contextBuilder.append("请基于操作系统通用知识谨慎回答，并明确告知学生此回答未引用教材原文，建议开启联网搜索或咨询老师。\n\n");
                }
            }

            if (webSearch) {
                contextBuilder.append("（已开启联网搜索，当前暂未接入外部搜索服务，仅基于教材内容回答）\n\n");
            }

            String userPrompt = contextBuilder + "学生问题：" + question;
            messages.add(new UserMessage(userPrompt));

            Response<AiMessage> response = chatModel.generate(messages);
            return response.content().text();
        } catch (Exception e) {
            log.error("AI回答失败", e);
            if (e.getMessage() != null && e.getMessage().contains("403")) {
                return "抱歉，AI服务余额不足，请联系管理员充值后使用。";
            }
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                return "抱歉，AI服务认证失败，请检查API Key配置。";
            }
            return "抱歉，AI回答时出现错误：" + e.getMessage();
        }
    }

    public String getCitation(String question, boolean webSearch) {
        List<String> relevantChunks = embeddingService.retrieve(question, 8);
        if (!relevantChunks.isEmpty()) {
            StringBuilder sb = new StringBuilder("📚 参考资料：\n");
            for (String chunk : relevantChunks) {
                if (chunk.contains("[来源:")) {
                    int start = chunk.indexOf("[来源:");
                    int end = chunk.indexOf("]", start);
                    if (end > start) {
                        sb.append("- ").append(chunk.substring(start + 4, end)).append("\n");
                    }
                }
            }
            if (sb.length() > "📚 参考资料：\n".length()) {
                return sb.toString().trim();
            }
            return "📚 参考资料：西电《操作系统》教材";
        }
        return null;
    }
}
