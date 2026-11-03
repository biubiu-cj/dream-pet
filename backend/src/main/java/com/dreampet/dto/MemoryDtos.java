package com.dreampet.dto;

import lombok.Data;

import java.util.List;

public final class MemoryDtos {

    private MemoryDtos() {
    }

    @Data
    public static class MemoryDTO {
        private Long id;
        private String memoryType;
        private String content;
        private Integer importance;
        private String createdAt;
    }

    @Data
    public static class MemorySummary {
        private String summary;
        private List<String> importantEvents;
        private List<String> interests;
        private List<String> profile;
    }
}