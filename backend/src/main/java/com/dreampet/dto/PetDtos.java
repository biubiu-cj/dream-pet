package com.dreampet.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

public final class PetDtos {

    private PetDtos() {
    }

    @Data
    public static class CreatePetRequest {
        @NotBlank(message = "不能为空")
        @Size(min = 1, max = 16, message = "名字需要 1~16 个字")
        private String name;

        @NotBlank(message = "不能为空")
        private String species;

        @NotBlank(message = "不能为空")
        private String personality;

        /** 可选：创建时不再要求选择兴趣，话题由聊天中自然提取 */
        @Size(max = 5, message = "最多 5 个兴趣")
        @Valid
        private List<@NotBlank(message = "不能为空")
        @Size(max = 32, message = "每个兴趣最多 32 字") String> interests;
    }

    @Data
    public static class UpdatePetRequest {
        @Size(min = 1, max = 16, message = "名字需要 1~16 个字")
        private String name;

        /** 更换伙伴种类：fox/rabbit/cat/bear/dino */
        private String species;

        private String personality;

        @Size(max = 5, message = "最多 5 个兴趣")
        @Valid
        private List<@NotBlank(message = "不能为空")
        @Size(max = 32, message = "每个兴趣最多 32 字") String> interests;
    }

    @Data
    public static class PetDTO {
        private Long id;
        private String name;
        private String species;
        private String speciesName;
        private String personality;
        private String personalityName;
        private Integer level;
        private Integer experience;
        private String growthStage;
        private String stageName;
        private String avatarKey;
        private Integer expToNextLevel;
        private Integer levelProgress;
        private List<String> interests;
        private String mood;
        private String createdAt;
    }
}