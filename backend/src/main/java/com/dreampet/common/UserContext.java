package com.dreampet.common;

/** 当前登录用户上下文（ThreadLocal），由 AuthInterceptor 写入 */
public class UserContext {
    private static final ThreadLocal<Long> CURRENT_USER = new ThreadLocal<>();

    public static void set(Long userId) {
        CURRENT_USER.set(userId);
    }

    public static Long get() {
        Long id = CURRENT_USER.get();
        if (id == null) {
            throw BizException.unauthorized("请先登录");
        }
        return id;
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
