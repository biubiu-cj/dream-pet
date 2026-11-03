package com.dreampet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dreampet.dto.GrowthDtos;
import com.dreampet.entity.GrowthEvent;
import com.dreampet.entity.Pet;
import com.dreampet.mapper.GrowthEventMapper;
import com.dreampet.mapper.PetMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 成长系统：经验、等级、成长阶段全部由后端计算，AI 无权决定。
 *
 * 等级阈值（累计经验）：Lv1=0, Lv2=30, Lv3=80, Lv4=150, Lv5=250,
 *                      Lv6=380, Lv7=550, Lv8=760, Lv9=1020, Lv10=1350
 * 阶段：Lv1~2 seed 初识伙伴 | Lv3~4 sprout 好奇伙伴 | Lv5~7 bloom 默契伙伴 | Lv8~10 dream 梦想伙伴
 */
@Service
@RequiredArgsConstructor
public class GrowthService {

    public static final int MAX_LEVEL = 10;
    private static final int[] THRESHOLDS = {0, 30, 80, 150, 250, 380, 550, 760, 1020, 1350};

    private final PetMapper petMapper;
    private final GrowthEventMapper eventMapper;

    @Data
    public static class GrowthResult {
        private int expGained;
        private boolean levelUp;
        private boolean stageUp;
        private int oldLevel;
        private int newLevel;
        private String oldStage;
        private String newStage;
    }

    public static int levelForExp(int exp) {
        int level = 1;
        for (int i = 0; i < THRESHOLDS.length; i++) {
            if (exp >= THRESHOLDS[i]) level = i + 1;
        }
        return Math.min(level, MAX_LEVEL);
    }

    public static String stageForLevel(int level) {
        if (level >= 8) return "dream";
        if (level >= 5) return "bloom";
        if (level >= 3) return "sprout";
        return "seed";
    }

    /** 距离下一级还需的经验；已满级返回 0 */
    public static int expToNextLevel(int exp) {
        int level = levelForExp(exp);
        if (level >= MAX_LEVEL) return 0;
        return THRESHOLDS[level] - exp;
    }

    /** 当前等级内的进度百分比（前端进度条） */
    public static int levelProgress(int exp) {
        int level = levelForExp(exp);
        if (level >= MAX_LEVEL) return 100;
        int base = THRESHOLDS[level - 1];
        int next = THRESHOLDS[level];
        return (int) Math.round(100.0 * (exp - base) / (next - base));
    }

    /**
     * 增加经验并处理升级/阶段变化。所有经验入口必须走这里。
     */
    @Transactional
    public GrowthResult addExp(Long userId, Pet pet, int amount, String eventType, String description) {
        GrowthResult result = new GrowthResult();
        result.setExpGained(Math.max(0, amount));
        result.setOldLevel(pet.getLevel());
        result.setOldStage(pet.getGrowthStage());

        if (amount > 0) {
            int oldLevel = pet.getLevel();
            String oldStage = pet.getGrowthStage();
            int newExp = pet.getExperience() + amount;
            int newLevel = levelForExp(newExp);
            String newStage = stageForLevel(newLevel);

            pet.setExperience(newExp);
            pet.setLevel(newLevel);
            pet.setGrowthStage(newStage);
            petMapper.updateById(pet);

            recordEvent(userId, pet.getId(), eventType, description, amount);

            result.setNewLevel(newLevel);
            result.setNewStage(newStage);
            result.setLevelUp(newLevel > oldLevel);
            result.setStageUp(!newStage.equals(oldStage));

            if (result.isLevelUp()) {
                recordEvent(userId, pet.getId(), "level_up",
                        "伙伴升到了 Lv." + newLevel + "！", 0);
            }
            if (result.isStageUp()) {
                recordEvent(userId, pet.getId(), "stage_up",
                        "伙伴进入了「" + stageLabel(newStage) + "」阶段！", 0);
            }
        } else {
            result.setNewLevel(pet.getLevel());
            result.setNewStage(pet.getGrowthStage());
        }
        return result;
    }

    /** 聊天经验：少量、且受每日上限控制，防止刷经验 */
    @Transactional
    public GrowthResult addChatExp(Long userId, Pet pet, int perChatExp, int dailyCap) {
        int todayExp = eventMapper.sumTodayChatExp(userId);
        int amount = Math.min(perChatExp, Math.max(0, dailyCap - todayExp));
        if (amount <= 0) {
            GrowthResult r = new GrowthResult();
            r.setExpGained(0);
            r.setOldLevel(pet.getLevel());
            r.setNewLevel(pet.getLevel());
            r.setOldStage(pet.getGrowthStage());
            r.setNewStage(pet.getGrowthStage());
            return r;
        }
        return addExp(userId, pet, amount, "chat_exp", "和伙伴开心地聊天");
    }

    /**
     * 按天汇总成长事件（用于首页「最近几天」、成长日记）。
     * 聊天 +2 经验的流水不再逐条展示，只统计次数；高光事件最多 3 条。
     */
    public List<GrowthDtos.DailySummary> dailySummaries(Long userId, int days) {
        int safeDays = Math.min(30, Math.max(1, days));
        LocalDate startDate = LocalDate.now().minusDays(safeDays - 1L);
        List<GrowthEvent> events = eventMapper.selectList(new LambdaQueryWrapper<GrowthEvent>()
                .eq(GrowthEvent::getUserId, userId)
                .ge(GrowthEvent::getCreatedAt, startDate.atStartOfDay())
                .orderByDesc(GrowthEvent::getCreatedAt)
                .orderByDesc(GrowthEvent::getId));

        Map<LocalDate, List<GrowthEvent>> byDay = new LinkedHashMap<>();
        for (GrowthEvent event : events) {
            if (event.getCreatedAt() == null) continue;
            byDay.computeIfAbsent(event.getCreatedAt().toLocalDate(), key -> new ArrayList<>()).add(event);
        }

        List<GrowthDtos.DailySummary> result = new ArrayList<>();
        for (Map.Entry<LocalDate, List<GrowthEvent>> entry : byDay.entrySet()) {
            DailyAccumulator acc = new DailyAccumulator(entry.getKey());
            for (GrowthEvent event : entry.getValue()) {
                acc.eventCount++;
                acc.experience += event.getExperienceGained() == null ? 0 : event.getExperienceGained();
                String type = event.getEventType();
                if ("chat_exp".equals(type)) {
                    acc.chatCount++;
                    continue;
                }
                if ("task_completed".equals(type)) acc.taskCount++;
                if ("level_up".equals(type) || "stage_up".equals(type)) acc.milestone = true;
                if (acc.highlights.size() < 3 && !acc.highlightSet.contains(event.getDescription())) {
                    acc.highlights.add(event.getDescription());
                    acc.highlightSet.add(event.getDescription());
                }
            }
            result.add(acc.toDTO());
        }
        return result;
    }

    private static final class DailyAccumulator {
        private final LocalDate date;
        private int eventCount;
        private int experience;
        private int chatCount;
        private int taskCount;
        private boolean milestone;
        private final List<String> highlights = new ArrayList<>();
        private final Set<String> highlightSet = new LinkedHashSet<>();

        private DailyAccumulator(LocalDate date) {
            this.date = date;
        }

        private GrowthDtos.DailySummary toDTO() {
            GrowthDtos.DailySummary dto = new GrowthDtos.DailySummary();
            dto.setDate(date.toString());
            dto.setEventCount(eventCount);
            dto.setExperience(experience);
            dto.setChatCount(chatCount);
            dto.setTaskCount(taskCount);
            dto.setHighlights(highlights);
            dto.setMilestone(milestone);
            return dto;
        }
    }

    public void recordEvent(Long userId, Long petId, String eventType, String description, int exp) {
        GrowthEvent e = new GrowthEvent();
        e.setUserId(userId);
        e.setPetId(petId);
        e.setEventType(eventType);
        e.setDescription(description);
        e.setExperienceGained(exp);
        eventMapper.insert(e);
    }

    public static String stageLabel(String stage) {
        return switch (stage) {
            case "sprout" -> "好奇伙伴";
            case "bloom" -> "默契伙伴";
            case "dream" -> "梦想伙伴";
            default -> "初识伙伴";
        };
    }
}
