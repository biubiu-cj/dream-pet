package com.dreampet.service;

import com.dreampet.common.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单的内存版调用限流（单实例部署足够，不引入 Redis）。
 * 用于防止公网部署时 AI 调用被刷爆，保护免费资源点与大模型费用。
 * 计数按自然小时窗口，重启后重置。
 */
@Slf4j
@Service
public class RateLimitService {

    /** key(action:userId) -> [epochHour, count] */
    private final Map<String, long[]> counters = new ConcurrentHashMap<>();

    public void check(Long userId, String action, int limitPerHour) {
        if (limitPerHour <= 0) return;
        long hour = System.currentTimeMillis() / 3_600_000L;
        String key = action + ":" + userId;
        long[] counter = counters.compute(key, (k, v) ->
                (v == null || v[0] != hour) ? new long[]{hour, 1} : new long[]{hour, v[1] + 1});
        if (counter[1] > limitPerHour) {
            log.info("限流触发: action={}, userId={}", action, userId);
            throw new BizException(429, "今天聊得有点多啦，休息一下，我们稍后再继续吧");
        }
        if (counters.size() > 5000) {
            counters.entrySet().removeIf(e -> e.getValue()[0] < hour - 1);
        }
    }
}