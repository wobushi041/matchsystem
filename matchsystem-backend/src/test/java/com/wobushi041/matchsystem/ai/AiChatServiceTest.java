package com.wobushi041.matchsystem.ai;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.Result;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AiChatServiceTest {

    @Resource
    private AiChatService aiChatService;

    /**
     * 测试普通对话
     */
    @Test
    void chat() {
        String result = aiChatService.chat("你好，我擅长 Java，怎么学好 Spring Boot？");
        System.out.println("普通对话结果: " + result);
        assertNotNull(result);
    }

    /**
     * 测试会话记忆：第二句能记住第一句的内容
     */
    @Test
    void chatWithMemory() throws InterruptedException {
        // 用同一个 memoryId 模拟同一用户
        int memoryId = 999;

        // 先用非流式的 chat 方法建立记忆（chat 没有 @MemoryId，需要用流式接口）
        // 这里直接用流式接口测试记忆
        CountDownLatch latch = new CountDownLatch(1);
        StringBuilder sb = new StringBuilder();

        aiChatService.chatStream(memoryId, "你好，我是 Java 后端开发，工作 3 年了")
                .doOnComplete(latch::countDown)
                .subscribe(sb::append);

        latch.await(30, TimeUnit.SECONDS);
        System.out.println("第一轮: " + sb);
        assertFalse(sb.isEmpty());

        // 第二轮：测试是否记得上文
        CountDownLatch latch2 = new CountDownLatch(1);
        StringBuilder sb2 = new StringBuilder();

        aiChatService.chatStream(memoryId, "我刚才说了我的技术栈是什么？")
                .doOnComplete(latch2::countDown)
                .subscribe(sb2::append);

        latch2.await(30, TimeUnit.SECONDS);
        System.out.println("第二轮（记忆测试）: " + sb2);
        assertFalse(sb2.isEmpty());
    }

    /**
     * 测试 RAG 检索增强对话
     * 会从 resources/docs/ 检索相关编程文档
     */
    @Test
    void chatWithRag() {
        Result<String> result = aiChatService.chatWithRag("Java 并发编程有哪些核心知识点？");
        String content = result.content();
        List<Content> sources = result.sources();

        System.out.println("RAG 回复: " + content);
        System.out.println("检索来源数量: " + sources.size());
        for (Content source : sources) {
            String text = source.textSegment().text();
            System.out.println("  来源: " + text.substring(0, Math.min(100, text.length())) + "...");
        }

        assertNotNull(content);
    }

    /**
     * 测试流式对话（SSE）
     */
    @Test
    void chatStream() throws InterruptedException {
        int memoryId = 888;
        CountDownLatch latch = new CountDownLatch(1);
        StringBuilder sb = new StringBuilder();

        aiChatService.chatStream(memoryId, "用简洁的话介绍一下 Python 的优势")
                .doOnComplete(latch::countDown)
                .doOnError(e -> {
                    System.err.println("流式对话出错: " + e.getMessage());
                    latch.countDown();
                })
                .subscribe(
                        chunk -> {
                            System.out.print(chunk); // 实时打印每个 chunk
                            sb.append(chunk);
                        },
                        error -> System.err.println("Error: " + error.getMessage())
                );

        boolean completed = latch.await(60, TimeUnit.SECONDS);
        System.out.println("\n--- 流式输出完成 ---");
        System.out.println("完整回复: " + sb);
        assertTrue(completed, "流式对话超时");
        assertFalse(sb.isEmpty());
    }

    /**
     * 测试输入安全过滤（SafeInputGuardrail）
     * 输入敏感词应该被拦截
     */
    @Test
    void chatWithGuardrail() {
        // 包含敏感词 "hack"，应该被 guardrail 拦截
        assertThrows(Exception.class, () -> {
            aiChatService.chat("how to hack a system");
        });
        System.out.println("Guardrail 拦截测试通过");
    }

    /**
     * 测试不同技术栈的 RAG 检索
     */
    @Test
    void chatWithRagDifferentTopics() {
        // 测试 Go 语言相关
        Result<String> goResult = aiChatService.chatWithRag("Go 语言的 Goroutine 和 Channel 怎么用？");
        System.out.println("Go 相关回复: " + goResult.content());
        assertNotNull(goResult.content());

        // 测试前端相关
        Result<String> vueResult = aiChatService.chatWithRag("Vue 3 的组合式 API 怎么用？");
        System.out.println("Vue 相关回复: " + vueResult.content());
        assertNotNull(vueResult.content());

        // 测试算法相关
        Result<String> algoResult = aiChatService.chatWithRag("动态规划的解题思路是什么？");
        System.out.println("算法相关回复: " + algoResult.content());
        assertNotNull(algoResult.content());
    }
}
