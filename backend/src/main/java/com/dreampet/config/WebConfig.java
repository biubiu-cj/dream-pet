package com.dreampet.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final AppProperties props;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/auth/guest",
                        "/api/system/info",
                        "/api/health"
                );
        // 注意：H2 控制台（/h2-console）由 Spring Boot 注册为独立 Servlet，
        // 不经过 DispatcherServlet，MVC 拦截器对它无效，列在这里也不会生效。
        // 本项目未引入 spring-boot-starter-security，因此没有额外的安全兜底：
        // 控制台的唯一开关是 profile 配置，生产由 application-prod.yml 显式关闭，
        // 并由 StartupSecurityValidator 在启动时强制校验。
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String origins = props.getCors().getAllowedOrigins();
        // 生产空配置会在 StartupSecurityValidator 被拒绝；这里兜底为「不注册任何 CORS 来源」，
        // 浏览器会阻止所有跨域请求，避免因配置缺失意外放开全站访问。
        if (origins == null || origins.isBlank()) {
            return;
        }
        String[] patterns = Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        if (patterns.length == 0) {
            return;
        }

        registry.addMapping("/**")
                .allowedOriginPatterns(patterns)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                // 鉴权走 Authorization 头（见 frontend/src/api/http.ts），不依赖 Cookie，
                // 因此无需放开凭据传递。
                .allowCredentials(false)
                .maxAge(3600);
    }
}
