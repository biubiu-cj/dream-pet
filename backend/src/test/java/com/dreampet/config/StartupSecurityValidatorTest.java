package com.dreampet.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 生产启动安全自检测试。
 *
 * <p>验证「宁可起不来，也不要带着不安全配置上线」：默认密钥、通配 CORS、
 * H2 数据源或 H2 控制台在生产环境下都必须阻止启动。
 */
class StartupSecurityValidatorTest {

    private AppProperties safeProps() {
        AppProperties props = new AppProperties();
        props.getAuth().setSecret("a-sufficiently-long-random-production-secret");
        props.getCors().setAllowedOrigins("https://dreampet.example.com");
        return props;
    }

    private MockEnvironment prodEnv() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.setProperty("spring.datasource.url", "jdbc:postgresql://db:5432/dreampet");
        return env;
    }

    private IllegalStateException assertRejected(AppProperties props, MockEnvironment env) {
        return assertThrows(IllegalStateException.class,
                () -> new StartupSecurityValidator(props, env).validate());
    }

    @Test
    @DisplayName("生产环境：配置齐全时正常通过")
    void passesWithProperProductionConfig() {
        assertDoesNotThrow(() -> new StartupSecurityValidator(safeProps(), prodEnv()).validate());
    }

    @Test
    @DisplayName("生产环境：仍使用仓库公开的默认密钥 -> 拒绝启动")
    void rejectsDefaultSecret() {
        AppProperties props = safeProps();
        props.getAuth().setSecret(StartupSecurityValidator.DEFAULT_SECRET);

        assertTrue(assertRejected(props, prodEnv()).getMessage().contains("AUTH_SECRET"));
    }

    @Test
    @DisplayName("生产环境：仍使用 .env.example 的公开占位密钥 -> 拒绝启动")
    void rejectsExamplePlaceholderSecret() {
        AppProperties props = safeProps();
        props.getAuth().setSecret(StartupSecurityValidator.EXAMPLE_PLACEHOLDER_SECRET);

        assertTrue(assertRejected(props, prodEnv()).getMessage().contains("AUTH_SECRET"));
    }

    @Test
    @DisplayName("生产环境：密钥为空 -> 拒绝启动")
    void rejectsBlankSecret() {
        AppProperties props = safeProps();
        props.getAuth().setSecret("");

        assertTrue(assertRejected(props, prodEnv()).getMessage().contains("AUTH_SECRET"));
    }

    @Test
    @DisplayName("生产环境：密钥过短 -> 拒绝启动")
    void rejectsShortSecret() {
        AppProperties props = safeProps();
        props.getAuth().setSecret("short");

        assertTrue(assertRejected(props, prodEnv()).getMessage().contains("过短"));
    }

    @Test
    @DisplayName("生产环境：CORS 通配 * -> 拒绝启动")
    void rejectsWildcardCors() {
        AppProperties props = safeProps();
        props.getCors().setAllowedOrigins("*");

        assertTrue(assertRejected(props, prodEnv()).getMessage().contains("CORS"));
    }

    @Test
    @DisplayName("生产环境：CORS 列表中混入 * -> 拒绝启动")
    void rejectsWildcardAmongOrigins() {
        AppProperties props = safeProps();
        props.getCors().setAllowedOrigins("https://a.example.com, *");

        assertTrue(assertRejected(props, prodEnv()).getMessage().contains("CORS"));
    }

    @Test
    @DisplayName("生产环境：CORS 未配置 -> 拒绝启动")
    void rejectsBlankCors() {
        AppProperties props = safeProps();
        props.getCors().setAllowedOrigins("");

        assertTrue(assertRejected(props, prodEnv()).getMessage().contains("CORS"));
    }

    @Test
    @DisplayName("生产环境：误用 H2 数据源（漏传 prod profile 的典型症状）-> 拒绝启动")
    void rejectsH2DataSourceInProduction() {
        MockEnvironment env = prodEnv();
        env.setProperty("spring.datasource.url", "jdbc:h2:file:./data/dreampet");

        assertTrue(assertRejected(safeProps(), env).getMessage().contains("H2"));
    }

    @Test
    @DisplayName("生产环境：H2 控制台被启用 -> 拒绝启动")
    void rejectsH2ConsoleInProduction() {
        MockEnvironment env = prodEnv();
        env.setProperty("spring.h2.console.enabled", "true");

        assertTrue(assertRejected(safeProps(), env).getMessage().contains("控制台"));
    }

    @Test
    @DisplayName("local + prod 同时激活：仍按生产校验，默认密钥必须被拒绝")
    void rejectsInsecureConfigWhenLocalAndProdBothActive() {
        MockEnvironment env = prodEnv();
        env.setActiveProfiles("local", "prod");

        AppProperties props = new AppProperties(); // 默认密钥 + 通配 CORS
        assertTrue(assertRejected(props, env).getMessage().contains("AUTH_SECRET"));
    }

    @Test
    @DisplayName("local profile：保留宽松默认值，不阻塞本地开发")
    void skipsValidationForLocalProfile() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("local");
        env.setProperty("spring.datasource.url", "jdbc:h2:file:./data/dreampet");
        env.setProperty("spring.h2.console.enabled", "true");

        AppProperties props = new AppProperties(); // 默认密钥 + 通配 CORS
        assertDoesNotThrow(() -> new StartupSecurityValidator(props, env).validate());
    }

    @Test
    @DisplayName("test profile：同样跳过严格校验")
    void skipsValidationForTestProfile() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");

        assertDoesNotThrow(() -> new StartupSecurityValidator(new AppProperties(), env).validate());
    }

    @Test
    @DisplayName("未显式指定 profile 时按非生产处理，避免本地裸启动被卡住")
    void treatsNoProfileAsNonProduction() {
        MockEnvironment env = new MockEnvironment();

        assertDoesNotThrow(() -> new StartupSecurityValidator(new AppProperties(), env).validate());
    }
}
