package com.wobushi041.matchsystem.controller;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.wobushi041.matchsystem.ai.AiChatService;
import com.wobushi041.matchsystem.model.domain.User;
import com.wobushi041.matchsystem.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * AI 编程助手控制器
 * 提供 SSE 流式对话接口，自动注入用户 tags 上下文
 */
@RestController
@RequestMapping("/ai")
@Slf4j
public class AiController {

    @Resource
    private AiChatService aiChatService;

    @Resource
    private UserService userService;

    @Resource
    private Gson gson;

    /**
     * SSE 流式对话
     * 自动读取当前用户的 tags，拼接到查询中增强 RAG 检索
     *
     * @param message 用户消息
     * @param request HTTP 请求（用于获取当前登录用户）
     * @return SSE 流式响应
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(@RequestParam(value = "message")String message,
                                              HttpServletRequest request) {
        // 1. 获取当前登录用户
        User loginUser = userService.getLoginUserFromRequest(request);
        int memoryId = (int) loginUser.getId();

        // 2. 读取用户 tags，拼接成上下文增强查询
        String enrichedMessage = buildEnrichedMessage(loginUser, message);

        // 3. 调用流式对话
        return aiChatService.chatStream(memoryId, enrichedMessage)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build())
                .doOnError(e -> log.error("AI 流式对话异常, userId={}", loginUser.getId(), e));
    }

    /**
     * 普通对话（非流式）
     */
    @PostMapping("/chat")
    public String chatSync(@RequestParam(value = "message" ) String message,
                           HttpServletRequest request) {
        User loginUser = userService.getLoginUserFromRequest(request);
        String enrichedMessage = buildEnrichedMessage(loginUser, message);
        return aiChatService.chat(enrichedMessage);
    }

    /**
     * 将用户 tags 拼接到消息中，增强 RAG 检索效果
     */
    private String buildEnrichedMessage(User user, String message) {
        List<String> tags = parseTags(user.getTags());
        if (tags.isEmpty()) {
            return message;
        }
        String username = Optional.ofNullable(user.getUsername())
                .orElse(user.getUserAccount());
        return "用户叫做" + username + "，用户偏向 " + String.join(", ", tags) + "，想深入学习编程。" + message;
    }

    /**
     * 解析用户 tags JSON 字符串
     */
    private List<String> parseTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            List<String> tags = gson.fromJson(tagsJson, new TypeToken<List<String>>() {}.getType());
            return tags != null ? tags : Collections.emptyList();
        } catch (Exception e) {
            log.warn("解析用户 tags 失败: {}", tagsJson, e);
            return Collections.emptyList();
        }
    }
}
