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

    private String knowledgeContext = "";

    public RagService(ChatLanguageModel chatModel) {
        this.chatModel = chatModel;
    }

    private static final String SYSTEM_PROMPT = """
            你是西安电子科技大学《操作系统》课程的AI答疑助教。你的职责是：
            1. 严格基于提供的教材内容回答问题，确保答案准确无误。
            2. 如果教材中没有相关内容，请明确告知学生"教材中未找到相关内容"，并建议查阅其他资料或咨询老师。
            3. 回答时尽量使用教材原文的术语和表述，保持学术严谨性。
            4. 如果提供了网络搜索结果，可以补充说明，但需明确标注来源。
            5. 不要回答与操作系统课程无关的问题。
            6. 回答格式清晰，使用Markdown格式，包含代码示例和公式时使用代码块。
            """;

    public String answer(String question, boolean webSearch) {
        try {
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new SystemMessage(SYSTEM_PROMPT));

            StringBuilder contextBuilder = new StringBuilder();
            if (!knowledgeContext.isEmpty()) {
                contextBuilder.append("以下是教材相关内容：\n").append(knowledgeContext).append("\n\n");
            }

            if (webSearch) {
                contextBuilder.append("（已开启联网搜索，但当前未接入搜索服务，仅基于教材内容回答）\n\n");
            }

            String userPrompt;
            if (contextBuilder.length() > 0) {
                userPrompt = contextBuilder + "学生问题：" + question;
            } else {
                userPrompt = "学生问题：" + question + "\n\n注意：当前知识库为空，请基于操作系统通用知识回答，并提示学生知识库尚未加载教材内容。";
            }

            messages.add(new UserMessage(userPrompt));

            Response<AiMessage> response = chatModel.generate(messages);
            return response.content().text();
        } catch (Exception e) {
            log.error("AI回答失败", e);
            if (e.getMessage() != null && e.getMessage().contains("403")) {
                return "抱歉，AI服务余额不足，请联系管理员充值后使用。";
            }
            return "抱歉，AI回答时出现错误：" + e.getMessage();
        }
    }

    public String getCitation(String question, boolean webSearch) {
        if (!knowledgeContext.isEmpty()) {
            return "📚 参考资料：西电《操作系统》教材";
        }
        return null;
    }

    public void setKnowledgeContext(String context) {
        this.knowledgeContext = context;
    }
}
