package com.wobushi041.matchsystem.ai.model;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * DeepSeek 模型配置
 * 通过 OpenAI 兼容接口接入 DeepSeek
 * DeepSeek模型参数通过@Value，yml注入
 * 核心：build，屏蔽底层实现，有ChatModel类和StreamingChatModel类
 */
@Configuration
@Slf4j
public class DeepSeekModelConfig {

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String apiKey;

    @Value("${langchain4j.open-ai.chat-model.model-name}")
    private String modelName;

    @Value("${langchain4j.open-ai.chat-model.base-url}")
    private String baseUrl;

    @Value("${langchain4j.open-ai.chat-model.temperature:0.7}")
    private double temperature;

    @Value("${langchain4j.open-ai.chat-model.max-tokens:2048}")
    private int maxTokens;

    @Resource
    private ChatModelListener chatModelListener;

    /**
     * 普通聊天模型
     */
    @Bean
    public ChatModel deepSeekChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .listeners(List.of(chatModelListener))
                .build();
    }

    /**
     * 流式聊天模型（SSE）
     */
    @Bean
    public StreamingChatModel deepSeekStreamingChatModel() {
        return OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .listeners(List.of(chatModelListener))
                .build();
    }
}
