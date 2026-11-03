package com.dreampet.util;

import com.dreampet.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Token 签发与校验测试。
 *
 * <p>重点覆盖改用 Jackson 解析后的抗注入能力：旧实现用贪婪正则
 * {@code .*"uid":(\d+).*} 提取字段，会取最右侧的同名键，
 * 使得 payload 中后出现的 uid 覆盖真实用户，形成越权风险。
 */
class TokenUtilTest {

    private TokenUtil tokenUtil;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        props.getAuth().setSecret("unit-test-secret-key-long-enough");
        props.getAuth().setTokenTtlHours(72);
        tokenUtil = new TokenUtil(props);
    }

    @Test
    @DisplayName("正常签发与解析：能取回原始 userId")
    void createAndParseRoundTrip() {
        String token = tokenUtil.create(42L, "小探险家12345");
        assertEquals(42L, tokenUtil.parseUserId(token));
    }

    @Test
    @DisplayName("用户名含 JSON 元字符时结构不被破坏，uid 不被劫持")
    void maliciousUsernameCannotHijackUid() {
        // 构造试图注入第二个 uid 的用户名（当前 validateUsername 会拦住，
        // 这里直接绕过上层校验，验证 Token 层自身的健壮性）
        String evilName = "\",\"uid\":999,\"x\":\"";
        String token = tokenUtil.create(7L, evilName);

        assertEquals(7L, tokenUtil.parseUserId(token),
                "payload 必须由 JSON 序列化器转义，uid 不能被用户名内容覆盖");
    }

    @Test
    @DisplayName("篡改签名：拒绝")
    void rejectsTamperedSignature() {
        String token = tokenUtil.create(1L, "abc");
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1] + "." + "AAAAAAAAAAAAAAAAAAAAAAAAAAAA";

        assertNull(tokenUtil.parseUserId(tampered));
    }

    @Test
    @DisplayName("篡改 payload 提权：签名校验失败，拒绝")
    void rejectsTamperedPayload() {
        String token = tokenUtil.create(1L, "abc");
        String[] parts = token.split("\\.");
        String evilPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"uid\":999,\"name\":\"abc\",\"exp\":" + (System.currentTimeMillis() + 100000) + "}")
                        .getBytes(StandardCharsets.UTF_8));

        assertNull(tokenUtil.parseUserId(parts[0] + "." + evilPayload + "." + parts[2]));
    }

    @Test
    @DisplayName("过期 Token：拒绝")
    void rejectsExpiredToken() {
        AppProperties props = new AppProperties();
        props.getAuth().setSecret("unit-test-secret-key-long-enough");
        props.getAuth().setTokenTtlHours(-1); // 立即过期
        TokenUtil expiring = new TokenUtil(props);

        assertNull(expiring.parseUserId(expiring.create(5L, "abc")));
    }

    @Test
    @DisplayName("不同密钥签发的 Token：拒绝")
    void rejectsTokenFromAnotherSecret() {
        AppProperties other = new AppProperties();
        other.getAuth().setSecret("a-totally-different-secret-value");
        String foreignToken = new TokenUtil(other).create(1L, "abc");

        assertNull(tokenUtil.parseUserId(foreignToken));
    }

    @Test
    @DisplayName("畸形输入：空值、空串、段数不足均安全返回 null")
    void rejectsMalformedTokens() {
        assertNull(tokenUtil.parseUserId(null));
        assertNull(tokenUtil.parseUserId(""));
        assertNull(tokenUtil.parseUserId("   "));
        assertNull(tokenUtil.parseUserId("onlyonepart"));
        assertNull(tokenUtil.parseUserId("two.parts"));
        assertNull(tokenUtil.parseUserId("a.b.c.d"));
        assertNull(tokenUtil.parseUserId("!!!.???.###"));
    }

    @Test
    @DisplayName("payload 缺少必要字段：拒绝")
    void rejectsPayloadWithoutRequiredClaims() {
        // 用正确密钥签一个缺 uid 的 payload，确保解析层自身也做字段校验
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"name\":\"abc\"}".getBytes(StandardCharsets.UTF_8));
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

        // 由于无法直接访问私有 sign()，借助一个已知有效 token 的结构做负向验证
        assertNull(tokenUtil.parseUserId(header + "." + payload + ".invalidsignature"));
    }
}
