package com.dreampet.llm;

import com.dreampet.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * OpenAI Chat Completions 兼容客户端（真实模型）。
 * 适配 DeepSeek / 智谱 / Moonshot / 通义 / OpenAI 等兼容接口。
 *
 * 失败处理：
 * - 区分 配置缺失 / 认证失败 / 限流 / 超时 / 上游 5xx / 空响应
 * - 超时与 5xx 自动重试 1 次（有限成本保护）
 * - 日志只记录状态码、错误类型与模型名，绝不输出 API Key 或响应正文
 */
@Slf4j
public class OpenAiCompatibleLlmClient implements LlmClient {

    private static final int MAX_ATTEMPTS = 2;

    private final AppProperties.Llm cfg;
    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public OpenAiCompatibleLlmClient(AppProperties.Llm cfg) {
        this.cfg = cfg;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(cfg.getConnectTimeoutMs()))
                .build();
    }

    @Override
    public String chat(List<LlmMessage> messages) {
        if (cfg.getApiKey() == null || cfg.getApiKey().isBlank()) {
            throw new LlmException(LlmException.Type.CONFIG, "LLM_API_KEY 为空");
        }

        LlmException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return sendOnce(messages);
            } catch (LlmException e) {
                lastError = e;
                if (!e.retryable() || attempt == MAX_ATTEMPTS) {
                    throw e;
                }
                log.warn("LLM 调用失败准备重试: type={}, attempt={}", e.getType(), attempt);
                sleepQuietly(600L * attempt);
            }
        }
        throw lastError != null ? lastError
                : new LlmException(LlmException.Type.UPSTREAM, "未知错误");
    }

    private String sendOnce(List<LlmMessage> messages) {
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", cfg.getModel());
            body.put("temperature", cfg.getTemperature());
            body.put("max_tokens", cfg.getMaxTokens());
            body.put("stream", false);
            ArrayNode arr = body.putArray("messages");
            for (LlmMessage m : messages) {
                ObjectNode node = arr.addObject();
                node.put("role", m.role());
                node.put("content", m.content());
            }

            String url = cfg.getBaseUrl().replaceAll("/+$", "") + "/chat/completions";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(cfg.getReadTimeoutMs()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + cfg.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();

            if (status == 200) {
                JsonNode root = mapper.readTree(response.body());
                JsonNode content = root.path("choices").path(0).path("message").path("content");
                if (content.isMissingNode() || content.asText().isBlank()) {
                    throw new LlmException(LlmException.Type.EMPTY, "模型返回空内容");
                }
                return content.asText().trim();
            }

            LlmException.Type type = switch (status) {
                case 401, 403 -> LlmException.Type.AUTH;
                case 429 -> LlmException.Type.RATE_LIMIT;
                case 408, 504 -> LlmException.Type.TIMEOUT;
                default -> status >= 500 ? LlmException.Type.UPSTREAM : LlmException.Type.UPSTREAM;
            };
            log.warn("LLM 调用失败: status={}, type={}, model={}", status, type, cfg.getModel());
            throw new LlmException(type, "HTTP " + status);
        } catch (LlmException e) {
            throw e;
        } catch (java.net.http.HttpTimeoutException e) {
            log.warn("LLM 调用超时: model={}, timeoutMs={}", cfg.getModel(), cfg.getReadTimeoutMs());
            throw new LlmException(LlmException.Type.TIMEOUT, "请求超时", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException(LlmException.Type.UPSTREAM, "请求被中断", e);
        } catch (Exception e) {
            // 网络异常 / JSON 解析异常等，只记录异常类型
            log.warn("LLM 调用异常: model={}, error={}", cfg.getModel(), e.getClass().getSimpleName());
            throw new LlmException(LlmException.Type.UPSTREAM, e.getClass().getSimpleName(), e);
        }
    }

    private void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}