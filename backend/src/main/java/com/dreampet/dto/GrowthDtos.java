package com.dreampet.dto;

import lombok.Data;

import java.util.List;

public final class GrowthDtos {

    private GrowthDtos() {
    }

    @Data
    public static class EventDTO {
        private Long id;
        private String eventType;
        private String description;
        private Integer experienceGained;
        private String createdAt;
    }

    @Data
    public static class DailySummary {
        /** yyyy-MM-dd */
        private String date;
        private int eventCount;
        private int experience;
        private int chatCount;
        private int taskCount;
        /** 当天高光时刻（任务完成、升级、阶段成长等，最多 3 条） */
        private List<String> highlights;
        /** 当天是否有升级/阶段晋级 */
        private boolean milestone;
    }

    @Data
    public static class AchievementDTO {
        private String key;
        private String title;
        private String description;
        private boolean unlocked;
        private int progress;
        private int target;
    }

    @Data
    public static class GrowthOverview {
        private PetDtos.PetDTO pet;
        private String stageDesc;
        private long completedTasks;
        private int totalExp;
        private int expToNextLevel;
        private int levelProgress;
        private List<String> interests;
        private List<EventDTO> recentEvents;
    }
}