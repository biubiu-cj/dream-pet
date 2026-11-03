package com.dreampet.config;

import com.dreampet.llm.LlmClient;
import com.dreampet.llm.MockLlmClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 测试专用：注入确定性的测试用 LLM 客户端（不访问网络、不产生费用）。
 * 生产运行时只存在真实模型客户端（见 LlmConfig）。
 */
@TestConfiguration
public class TestLlmConfig {

    @Bean
    @Primary
    public LlmClient testLlmClient() {
        return new MockLlmClient();
    }
}