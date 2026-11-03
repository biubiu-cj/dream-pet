package com.dreampet.service;

import com.dreampet.common.BizException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RateLimitServiceTest {

    @Test
    void rejectsWhenHourlyLimitExceeded() {
        RateLimitService service = new RateLimitService();
        service.check(1L, "chat", 2);
        service.check(1L, "chat", 2);
        BizException e = assertThrows(BizException.class, () -> service.check(1L, "chat", 2));
        assertEquals(429, e.getCode());

        // 其他用户不受影响
        service.check(2L, "chat", 2);
        // 其他动作独立计数
        service.check(1L, "task", 1);
    }
}