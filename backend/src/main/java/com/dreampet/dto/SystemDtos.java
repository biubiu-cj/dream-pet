package com.dreampet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

public final class SystemDtos {

    private SystemDtos() {
    }

    @Data
    @AllArgsConstructor
    public static class SystemInfo {
        private String mode;
        private String appName;
    }
}