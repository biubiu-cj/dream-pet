package com.dreampet.llm;

import com.dreampet.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 运行时只使用真实文本大模型（OpenAI 兼容接口）。
 * Mock 实现仅保留在测试源码中，由 TestLlmConfig 注入，不会进入生产运行时。
 */
@Slf4j
@Configuration
public class LlmConfig {

    @Bean
    @Profile("!test")
    public LlmClient llmClient(AppProperties props) {
        AppProperties.Llm cfg = props.getLlm();
        if (cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
            log.error("未配置 LLM_API_KEY，所有 AI 请求将返回 503。请通过环境变量设置 API Key。");
        }
        // 仅打印供应商与模型，绝不打印 Key
        log.info("LLM 真实模式: model={}, baseUrl={}", cfg.getModel(), cfg.getBaseUrl());
        return new OpenAiCompatibleLlmClient(cfg);
    }
}