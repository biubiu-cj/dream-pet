package com.dreampet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

public final class AuthDtos {

    private AuthDtos() {
    }

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "不能为空")
        @Pattern(regexp = "^[a-zA-Z0-9_\\u4e00-\\u9fa5]{2,20}$", message = "需要 2~20 位，仅限中英文、数字和下划线")
        private String username;

        @NotBlank(message = "不能为空")
        @Size(min = 6, max = 64, message = "密码需要 6~64 位")
        private String password;
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "不能为空")
        private String username;

        @NotBlank(message = "不能为空")
        private String password;
    }

    @Data
    public static class AuthResponse {
        private String token;
        private String username;
        private Long userId;
        private boolean hasPet;
    }

    @Data
    public static class MeResponse {
        private Long userId;
        private String username;
        private boolean hasPet;
    }
}