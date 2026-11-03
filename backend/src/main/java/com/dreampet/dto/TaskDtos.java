package com.dreampet.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

public final class TaskDtos {

    private TaskDtos() {
    }

    @Data
    public static class TaskDTO {
        private Long id;
        private String title;
        private String description;
        private String category;
        private Integer difficulty;
        private Integer reward;
        private String status;
        private String createdAt;
        private String completedAt;
    }

    @Data
    public static class TodayTasksResponse {
        private List<TaskDTO> tasks;
        private boolean hasActive;
    }

    @Data
    public static class CompleteTaskRequest {
        @Size(max = 500, message = "分享最多 500 字")
        private String share;
    }

    @Data
    public static class CompleteTaskResponse {
        private PetDtos.PetDTO pet;
        private int xpGained;
        private boolean levelUp;
        private int newLevel;
        private boolean stageUp;
        private String newStage;
        private String feedback;
    }
}