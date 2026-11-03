package com.dreampet.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 生产环境启动安全自检（fail-fast）。
 *
 * <p>实现为 {@link InitializingBean}：在 Bean 初始化阶段即校验，
 * 早于 Web 容器开始监听端口，避免出现「端口已开放但配置不安全」的窗口。
 *
 * <p>设计原则：宁可起不来，也不要带着「看起来安全」的错误状态在线上运行。
 * 本类只做校验，不修改任何业务行为；仅在「只激活了 local/test」或「未激活任何 profile」时跳过。
 *
 * <p>校验项：
 * <ol>
 *   <li>AUTH_SECRET 必须显式配置，且不能是仓库里公开的默认值</li>
 *   <li>CORS 必须显式指定可信 Origin，不允许通配 *</li>
 *   <li>不允许以 local profile 或 H2 数据源 / H2 控制台上生产</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StartupSecurityValidator implements InitializingBean {

    /** 与 AppProperties.Auth#secret、application.yml 中的默认值保持一致 */
    public static final String DEFAULT_SECRET = "dreampet-local-dev-secret-change-me";

    /** 曾出现在 .env.example 中的公开占位值，同样不能用于生产 */
    public static final String EXAMPLE_PLACEHOLDER_SECRET = "please-change-me-to-a-long-random-secret";

    /** 已知的公开/占位密钥，一律拒绝 */
    private static final java.util.Set<String> INSECURE_SECRETS =
            java.util.Set.of(DEFAULT_SECRET, EXAMPLE_PLACEHOLDER_SECRET);

    /** 仅这些 profile 视为「非生产」，可以使用宽松默认值 */
    private static final List<String> NON_PROD_PROFILES = List.of("local", "test");

    private static final int MIN_SECRET_LENGTH = 16;

    private final AppProperties props;
    private final Environment environment;

    @Override
    public void afterPropertiesSet() {
        validate();
    }

    /** 独立方法便于单元测试直接调用 */
    public void validate() {
        if (!isProduction()) {
            log.info("启动安全自检：当前为非生产 profile({})，跳过严格校验",
                    Arrays.toString(environment.getActiveProfiles()));
            return;
        }

        // 1. 认证密钥
        String secret = props.getAuth().getSecret();
        if (secret == null || secret.isBlank()) {
            throw fail("AUTH_SECRET 未配置。生产环境必须通过环境变量 AUTH_SECRET 注入 Token 签名密钥。");
        }
        if (INSECURE_SECRETS.contains(secret.trim())) {
            throw fail("AUTH_SECRET 仍为仓库中公开的默认值或示例占位值。任何人都能据此伪造登录凭证，"
                    + "请通过环境变量 AUTH_SECRET 注入一个随机密钥。");
        }
        if (secret.trim().length() < MIN_SECRET_LENGTH) {
            throw fail("AUTH_SECRET 过短（当前 " + secret.trim().length()
                    + " 位，至少需要 " + MIN_SECRET_LENGTH + " 位）。");
        }

        // 2. CORS
        String origins = props.getCors().getAllowedOrigins();
        if (origins == null || origins.isBlank()) {
            throw fail("CORS_ALLOWED_ORIGINS 未配置。生产环境必须显式指定前端域名。");
        }
        if (Arrays.stream(origins.split(",")).map(String::trim).anyMatch("*"::equals)) {
            throw fail("CORS_ALLOWED_ORIGINS 配置为通配 *。生产环境必须显式列出可信前端域名，"
                    + "例如 https://your-app.tcloudbaseapp.com。");
        }

        // 3. 数据源与 H2 控制台
        String jdbcUrl = environment.getProperty("spring.datasource.url", "");
        if (jdbcUrl.startsWith("jdbc:h2:")) {
            throw fail("生产环境检测到 H2 数据源（" + jdbcUrl + "）。"
                    + "这通常意味着漏传 SPRING_PROFILES_ACTIVE=prod，数据将无法持久化。");
        }
        if (environment.getProperty("spring.h2.console.enabled", Boolean.class, false)) {
            throw fail("生产环境检测到 H2 控制台已启用。该控制台不经过登录拦截器，"
                    + "暴露在公网等同于开放数据库，请关闭 spring.h2.console.enabled。");
        }

        log.info("启动安全自检通过：密钥已自定义、CORS 已限定、未使用 H2。");
    }

    /**
     * 当前是否为生产环境。
     * 只要激活了任意一个非 local / test 的 profile，就按生产处理；
     * 同时激活 local 与 prod 时也必须严格校验，不能因为含有 local 就跳过。
     * 未激活任何 profile 时按非生产处理（application.yml 默认注入 local，用于本地开发）。
     */
    private boolean isProduction() {
        String[] active = environment.getActiveProfiles();
        if (active.length == 0) {
            // 未显式激活任何 profile 时，以 spring.profiles.active 的解析结果为准
            String configured = environment.getProperty("spring.profiles.active", "");
            if (configured.isBlank()) {
                return false;
            }
            return Arrays.stream(configured.split(","))
                    .map(String::trim)
                    .filter(p -> !p.isEmpty())
                    .anyMatch(p -> !NON_PROD_PROFILES.contains(p));
        }
        return Arrays.stream(active).anyMatch(p -> !NON_PROD_PROFILES.contains(p));
    }

    private IllegalStateException fail(String reason) {
        String message = "\n"
                + "========================================================\n"
                + " DreamPet 启动安全自检未通过，服务已拒绝启动。\n"
                + " 原因：" + reason + "\n"
                + " 参考：README「环境变量」与 docs/DEPLOYMENT.md\n"
                + "========================================================";
        log.error(message);
        return new IllegalStateException(message);
    }
}
