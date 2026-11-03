package com.dreampet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

public final class ChatDtos {

    private ChatDtos() {
    }

    @Data
    public static class ChatRequest {
        @NotBlank(message = "说点什么吧")
        @Size(max = 500, message = "消息最多 500 字")
        private String message;
    }

    @Data
    public static class ChatResponse {
        private String reply;
        private PetDtos.PetDTO pet;
        private TaskDtos.TaskDTO suggestedTask;
        private boolean canSuggestTask;
        private String mood;
        private boolean demo;
        /** 本轮用户意图（intent code，例如 task_request / memory_recall） */
        private String intent;
        /** 意图中文标签 */
        private String intentLabel;
    }

    @Data
    public static class IntentResponse {
        private String intent;
        private String label;
    }

    @Data
    public static class ChatMessageDTO {
        private Long id;
        private String role;
        private String content;
        private String createdAt;
    }

    @Data
    public static class HistoryResponse {
        private List<ChatMessageDTO> messages;
        private String greeting;
    }
}