package com.dreampet.util;

import com.dreampet.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 轻量级 Token 工具：HS256 签名的简化 JWT（header.payload.signature）。
 * 不引入额外 JWT 依赖，满足本项目认证需求。
 *
 * <p>安全要点：
 * <ul>
 *   <li>payload 使用 Jackson 构造与解析，不做字符串拼接/正则提取，
 *       避免用户名等可变字段影响 JSON 结构（正则的贪婪匹配会取最右侧的同名字段）</li>
 *   <li>签名比较使用 {@link MessageDigest#isEqual} 恒定时间比较，避免时序侧信道</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenUtil {

    private static final String HEADER_B64 =
            Base64.getUrlEncoder().withoutPadding()
                    .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AppProperties props;

    public String create(Long userId, String username) {
        long exp = System.currentTimeMillis() + props.getAuth().getTokenTtlHours() * 3600_000L;
        String payloadJson;
        try {
            payloadJson = MAPPER.writeValueAsString(
                    MAPPER.createObjectNode()
                            .put("uid", userId)
                            .put("name", username)
                            .put("exp", exp));
        } catch (Exception e) {
            throw new IllegalStateException("Token 生成失败", e);
        }
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return HEADER_B64 + "." + payload + "." + sign(HEADER_B64 + "." + payload);
    }

    /** 校验并解析 userId，无效或过期返回 null */
    public Long parseUserId(String token) {
        if (token == null || token.isBlank()) return null;
        String[] parts = token.split("\\.");
        if (parts.length != 3) return null;
        if (!signatureMatches(parts[0] + "." + parts[1], parts[2])) return null;
        try {
            String json = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JsonNode node = MAPPER.readTree(json);
            JsonNode uid = node.get("uid");
            JsonNode exp = node.get("exp");
            if (uid == null || !uid.canConvertToLong() || exp == null || !exp.canConvertToLong()) {
                return null;
            }
            return exp.asLong() > System.currentTimeMillis() ? uid.asLong() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** 恒定时间比较签名，避免逐字节短路带来的时序差异 */
    private boolean signatureMatches(String data, String providedSignature) {
        byte[] expected = sign(data).getBytes(StandardCharsets.UTF_8);
        byte[] provided = providedSignature.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, provided);
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(props.getAuth().getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Token 签名失败", e);
        }
    }
}
