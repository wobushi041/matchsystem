package com.wobushi041.matchsystem.ai.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;

import java.util.Set;

/**
 * 输入安全过滤护轨
 * 检测用户输入中的敏感词，拦截不当内容
 */
public class SafeInputGuardrail implements InputGuardrail {

    private static final Set<String> SENSITIVE_WORDS = Set.of(
            "kill", "evil", "hack", "attack", "exploit",
            "暴力", "色情", "赌博", "毒品"
    );

    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        String inputText = userMessage.singleText().toLowerCase();
        String[] words = inputText.split("\\W+");
        for (String word : words) {
            if (SENSITIVE_WORDS.contains(word)) {
                return fatal("检测到敏感词: " + word);
            }
        }
        return success();
    }
}
