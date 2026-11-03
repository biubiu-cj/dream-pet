package com.dreampet.llm;

import java.util.List;

/**
 * LLM 客户端统一接口。
 * 实现：MockLlmClient（演示模式）/ OpenAiCompatibleLlmClient（真实模式）。
 */
public interface LlmClient {
    String chat(List<LlmMessage> messages);
}
