package com.dreampet.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "dreampet")
public class AppProperties {

    private Auth auth = new Auth();
    private Cors cors = new Cors();
    private Llm llm = new Llm();
    private Growth growth = new Growth();
    private Chat chat = new Chat();
    private RateLimit rateLimit = new RateLimit();

    @Data
    public static class Chat {
        /** 距上次聊天超过该分钟数后，进入聊天页生成「欢迎回来」记忆问候 */
        private int greetingIdleMinutes = 30;
        /** 规则无法判断意图时，是否调用一次短 LLM 请求兜底识别 */
        private boolean intentLlmEnabled = true;
    }

    @Data
    public static class Auth {
        private String secret = "dreampet-local-dev-secret-change-me";
        private int tokenTtlHours = 72;
    }

    @Data
    public static class Cors {
        private String allowedOrigins = "*";
    }

    @Data
    public static class RateLimit {
        /** 每用户每小时最大聊天次数（内存计数，单实例有效） */
        private int chatPerHour = 40;
        /** 每用户每小时最大任务生成次数 */
        private int taskGeneratePerHour = 12;
        /** 意图识别调试接口每小时调用上限 */
        private int intentPerHour = 60;
    }

    @Data
    public static class Llm {
        private String apiKey = "";
        private String baseUrl = "https://api.deepseek.com/v1";
        private String model = "deepseek-chat";
        private int connectTimeoutMs = 10000;
        private int readTimeoutMs = 60000;
        private int maxTokens = 800;
        private double temperature = 0.8;
    }

    @Data
    public static class Growth {
        private int chatExp = 2;
        private int chatExpDailyCap = 20;
        private int createPetExp = 10;
        private int taskRewardMin = 5;
        private int taskRewardMax = 30;
    }
}
