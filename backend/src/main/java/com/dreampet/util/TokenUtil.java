package com.dreampet.util;

import com.dreampet.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 轻量级 Token 工具：HS256 签名的简化 JWT（header.payload.signature）。
 * 不引入额外 JWT 依赖，满足本项目认证需求。
 */
@Component
@RequiredArgsConstructor
public class TokenUtil {

    private static final String HEADER_B64 =
            Base64.getUrlEncoder().withoutPadding()
                    .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    private final AppProperties props;

    public String create(Long userId, String username) {
        long exp = System.currentTimeMillis() + props.getAuth().getTokenTtlHours() * 3600_000L;
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"uid\":" + userId + ",\"name\":\"" + username + "\",\"exp\":" + exp + "}")
                        .getBytes(StandardCharsets.UTF_8));
        return HEADER_B64 + "." + payload + "." + sign(HEADER_B64 + "." + payload);
    }

    /** 校验并解析 userId，无效或过期返回 null */
    public Long parseUserId(String token) {
        if (token == null || token.isBlank()) return null;
        String[] parts = token.split("\\.");
        if (parts.length != 3) return null;
        if (!sign(parts[0] + "." + parts[1]).equals(parts[2])) return null;
        try {
            String json = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            long uid = Long.parseLong(json.replaceAll(".*\"uid\":(\\d+).*", "$1"));
            long exp = Long.parseLong(json.replaceAll(".*\"exp\":(\\d+).*", "$1"));
            return exp > System.currentTimeMillis() ? uid : null;
        } catch (Exception e) {
            return null;
        }
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
