package com.dreampet.llm;

import com.dreampet.common.BizException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** LLM 失败映射与缓存行为单测（不访问网络） */
class LlmServiceTest {

    @Test
    void authFailureMapsTo502FriendlyMessage() {
        LlmClient failing = messages -> {
            throw new LlmException(LlmException.Type.AUTH, "HTTP 401");
        };
        LlmService service = new LlmService(failing);

        BizException e = assertThrows(BizException.class,
                () -> service.chat("system", List.of(LlmMessage.user("hi"))));
        assertEquals(502, e.getCode());
        assertTrue(e.getMessage().contains("认证"));
    }

    @Test
    void timeoutMapsTo504() {
        LlmClient failing = messages -> {
            throw new LlmException(LlmException.Type.TIMEOUT, "请求超时");
        };
        LlmService service = new LlmService(failing);

        BizException e = assertThrows(BizException.class, () -> service.complete("system"));
        assertEquals(504, e.getCode());
        assertTrue(e.getMessage().contains("太久了"));
    }

    @Test
    void configFailureMapsTo503() {
        LlmClient failing = messages -> {
            throw new LlmException(LlmException.Type.CONFIG, "LLM_API_KEY 为空");
        };
        LlmService service = new LlmService(failing);

        BizException e = assertThrows(BizException.class, () -> service.complete("system"));
        assertEquals(503, e.getCode());
        assertTrue(e.getMessage().contains("还没有配置"));
    }

    @Test
    void cachedPromptReusesResult() {
        AtomicInteger calls = new AtomicInteger();
        LlmClient counting = messages -> {
            calls.incrementAndGet();
            return "固定回复";
        };
        LlmService service = new LlmService(counting);

        assertEquals("固定回复", service.completeCached("同一个提示"));
        assertEquals("固定回复", service.completeCached("同一个提示"));
        assertEquals(1, calls.get());
        service.completeCached("另一个提示");
        assertEquals(2, calls.get());
    }
}