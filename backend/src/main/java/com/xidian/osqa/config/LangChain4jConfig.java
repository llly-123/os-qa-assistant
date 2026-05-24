package com.xidian.osqa.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LangChain4jConfig {

    @Value("${langchain4j.deepseek.api-key}")
    private String apiKey;

    @Value("${langchain4j.deepseek.model-name}")
    private String modelName;

    @Value("${langchain4j.deepseek.base-url}")
    private String baseUrl;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .timeout(Duration.ofSeconds(120))
                .build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName("text-embedding-v3")
                .baseUrl(baseUrl)
                .timeout(Duration.ofSeconds(60))
                .build();
    }
}
