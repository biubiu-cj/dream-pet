package com.dreampet.llm;

import lombok.Getter;

/**
 * 大模型调用异常：区分配置/认证/限流/超时/上游故障，
 * 携带面向用户的友好文案与对应 HTTP 状态码，便于接口统一输出。
 * 注意：detail 仅用于日志，不得包含 API Key 等敏感信息。
 */
@Getter
public class LlmException extends RuntimeException {

    public enum Type {
        CONFIG(503, "AI 服务还没有配置好，请联系管理员"),
        AUTH(502, "AI 服务认证失败，请检查 API Key 配置"),
        RATE_LIMIT(502, "AI 请求太频繁啦，请稍等一会儿再试"),
        TIMEOUT(504, "AI 伙伴想得太久了，请稍后再试一次"),
        UPSTREAM(502, "AI 服务暂时不可用，请稍后再试"),
        EMPTY(502, "AI 伙伴没有回应，请再说一次");

        private final int httpCode;
        private final String userMessage;

        Type(int httpCode, String userMessage) {
            this.httpCode = httpCode;
            this.userMessage = userMessage;
        }

        public int getHttpCode() {
            return httpCode;
        }

        public String getUserMessage() {
            return userMessage;
        }
    }

    private final Type type;

    public LlmException(Type type, String detail) {
        super(detail);
        this.type = type;
    }

    public LlmException(Type type, String detail, Throwable cause) {
        super(detail, cause);
        this.type = type;
    }

    public boolean retryable() {
        return type == Type.TIMEOUT || type == Type.UPSTREAM;
    }

    public int httpCode() {
        return type.getHttpCode();
    }

    public String userMessage() {
        return type.getUserMessage();
    }
}