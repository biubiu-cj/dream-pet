package com.dreampet.llm;

import com.dreampet.common.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LLM 统一门面：
 * - 只使用真实 OpenAI 兼容文本模型（全局唯一供应商，控制成本）
 * - 相同 Prompt 的短时缓存复用（任务生成、欢迎语等场景避免重复计费）
 * - 统一异常转换：LlmException → 带业务错误码 / HTTP 状态码的友好提示
 */
@Slf4j
@Service
public class LlmService {

    private final LlmClient client;

    /** 简单 TTL 缓存：key -> [expireAt, value] */
    private final Map<String, Object[]> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 10 * 60 * 1000L;
    private static final int CACHE_MAX_SIZE = 300;

    public LlmService(LlmClient client) {
        this.client = client;
    }

    /** 直接调用 */
    public String chat(String systemPrompt, List<LlmMessage> history) {
        List<LlmMessage> messages = new java.util.ArrayList<>();
        messages.add(LlmMessage.system(systemPrompt));
        if (history != null) messages.addAll(history);
        try {
            return client.chat(messages);
        } catch (LlmException e) {
            // 仅记录错误类型与脱敏细节，不输出请求内容与 API Key
            log.warn("LLM 调用失败: type={}, detail={}", e.getType(), e.getMessage());
            throw new BizException(e.httpCode(), e.userMessage());
        }
    }

    /** 单系统提示调用（任务生成/摘要/反馈等一次性请求） */
    public String complete(String systemPrompt) {
        return chat(systemPrompt, List.of());
    }

    /** 带缓存调用：相同 Prompt 在 TTL 内复用结果，避免重复计费 */
    public String completeCached(String systemPrompt) {
        long now = System.currentTimeMillis();
        cache.values().removeIf(v -> (long) v[0] < now);
        if (cache.size() > CACHE_MAX_SIZE) cache.clear();

        String key = sha256(systemPrompt);
        Object[] hit = cache.get(key);
        if (hit != null && (long) hit[0] > now) {
            return (String) hit[1];
        }
        String result = complete(systemPrompt);
        cache.put(key, new Object[]{now + CACHE_TTL_MS, result});
        return result;
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(s.hashCode());
        }
    }
}