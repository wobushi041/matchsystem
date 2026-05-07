package com.wobushi041.matchsystem.ai;

import com.wobushi041.matchsystem.ai.guardrail.SafeInputGuardrail;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.Result;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.guardrail.InputGuardrails;
import reactor.core.publisher.Flux;

/**
 * AI 编程助手服务接口
 * 基于 LangChain4j AiServices 声明式定义
 */
@InputGuardrails({SafeInputGuardrail.class})
public interface AiChatService {

    /**
     * 流式对话（SSE）
     *
     * @param memoryId   会话 ID，用于隔离不同用户的对话记忆
     * @param userMessage 用户消息（已注入 tags 上下文）
     * @return 流式响应
     */
    @SystemMessage(fromResource = "system-prompt.txt")
    Flux<String> chatStream(@MemoryId int memoryId, @UserMessage String userMessage);

    /**
     * 普通对话（非流式）
     *
     * @param userMessage 用户消息
     * @return AI 回复
     */
    @SystemMessage(fromResource = "system-prompt.txt")
    String chat(String userMessage);

    /**
     * RAG 增强对话（返回检索来源信息）
     *
     * @param userMessage 用户消息
     * @return AI 回复及检索来源
     */
    @SystemMessage(fromResource = "system-prompt.txt")
    Result<String> chatWithRag(String userMessage);
}
