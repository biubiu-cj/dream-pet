package com.dreampet.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dreampet.common.ApiResponse;
import com.dreampet.common.UserContext;
import com.dreampet.dto.GrowthDtos;
import com.dreampet.dto.PetDtos;
import com.dreampet.entity.GrowthEvent;
import com.dreampet.entity.Task;
import com.dreampet.mapper.GrowthEventMapper;
import com.dreampet.mapper.TaskMapper;
import com.dreampet.service.AchievementService;
import com.dreampet.service.GrowthService;
import com.dreampet.service.PetService;
import com.dreampet.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/growth")
@RequiredArgsConstructor
public class GrowthController {

    private final PetService petService;
    private final TaskMapper taskMapper;
    private final GrowthEventMapper eventMapper;
    private final GrowthService growthService;
    private final AchievementService achievementService;

    @GetMapping("/overview")
    public ApiResponse<GrowthDtos.GrowthOverview> overview() {
        Long userId = UserContext.get();
        PetDtos.PetDTO pet = petService.current(userId);
        long completed = taskMapper.selectCount(new LambdaQueryWrapper<Task>()
                .eq(Task::getUserId, userId)
                .eq(Task::getStatus, "completed"));

        GrowthDtos.GrowthOverview overview = new GrowthDtos.GrowthOverview();
        overview.setPet(pet);
        overview.setStageDesc(stageDesc(pet.getGrowthStage()));
        overview.setCompletedTasks(completed);
        overview.setTotalExp(pet.getExperience());
        overview.setExpToNextLevel(GrowthService.expToNextLevel(pet.getExperience()));
        overview.setLevelProgress(GrowthService.levelProgress(pet.getExperience()));
        overview.setInterests(pet.getInterests());
        overview.setRecentEvents(events(userId, 5));
        return ApiResponse.ok(overview);
    }

    @GetMapping("/events")
    public ApiResponse<List<GrowthDtos.EventDTO>> events(@RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.ok(events(UserContext.get(), limit));
    }

    /** 按天聚合的成长回顾（聊天经验流水已合并为次数统计） */
    @GetMapping("/daily")
    public ApiResponse<List<GrowthDtos.DailySummary>> daily(@RequestParam(defaultValue = "7") int days) {
        return ApiResponse.ok(growthService.dailySummaries(UserContext.get(), days));
    }

    /** 成就徽章（基于已有数据实时计算） */
    @GetMapping("/achievements")
    public ApiResponse<List<GrowthDtos.AchievementDTO>> achievements() {
        return ApiResponse.ok(achievementService.list(UserContext.get()));
    }

    private List<GrowthDtos.EventDTO> events(Long userId, int limit) {
        int safeLimit = Math.min(100, Math.max(1, limit));
        return eventMapper.selectList(new LambdaQueryWrapper<GrowthEvent>()
                        .eq(GrowthEvent::getUserId, userId)
                        .orderByDesc(GrowthEvent::getCreatedAt)
                        .orderByDesc(GrowthEvent::getId)
                        .last("LIMIT " + safeLimit))
                .stream().map(this::toDTO).toList();
    }

    private GrowthDtos.EventDTO toDTO(GrowthEvent e) {
        GrowthDtos.EventDTO dto = new GrowthDtos.EventDTO();
        dto.setId(e.getId());
        dto.setEventType(e.getEventType());
        dto.setDescription(e.getDescription());
        dto.setExperienceGained(e.getExperienceGained());
        dto.setCreatedAt(TimeUtil.format(e.getCreatedAt()));
        return dto;
    }

    private String stageDesc(String stage) {
        return switch (stage == null ? "seed" : stage) {
            case "sprout" -> "好奇心让你们越来越亲近";
            case "bloom" -> "你们已经非常默契啦";
            case "dream" -> "一起追逐梦想的伙伴";
            default -> "刚刚相遇，一切才刚开始";
        };
    }
}