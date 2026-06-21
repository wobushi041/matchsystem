package com.wobushi041.matchsystem.ai;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 编程助手服务工厂
 * 手动组装 ChatMemory + RAG ContentRetriever + 流式模型
 */
@Configuration
public class AiChatServiceFactory {

    @Autowired
    private ChatModel deepSeekChatModel;

    @Autowired
    private StreamingChatModel deepSeekStreamingChatModel;

    @Autowired(required = false)
    private ContentRetriever contentRetriever;

    @Bean
    public AiChatService aiChatService() {
        var builder = AiServices.builder(AiChatService.class)
                // 聊天模型（普通 + 流式）
                .chatModel(deepSeekChatModel)
                .streamingChatModel(deepSeekStreamingChatModel)
                // 会话记忆：每个用户独立，最多保留 20 条消息，滑动窗口实现
                .chatMemoryProvider(memoryId ->
                        MessageWindowChatMemory.withMaxMessages(20));

        // RAG 检索增强（Ollama 不可用时跳过）
        if (contentRetriever != null) {
            builder.contentRetriever(contentRetriever);
        }

        return builder.build();
    }
}
