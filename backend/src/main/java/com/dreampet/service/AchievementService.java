package com.dreampet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dreampet.dto.GrowthDtos;
import com.dreampet.entity.GrowthEvent;
import com.dreampet.entity.Pet;
import com.dreampet.entity.Task;
import com.dreampet.entity.UserInterest;
import com.dreampet.mapper.GrowthEventMapper;
import com.dreampet.mapper.TaskMapper;
import com.dreampet.mapper.UserInterestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 成就徽章：全部基于已有数据实时计算，不新增表、不产生额外调用。
 * 不设计签到/连续打卡压力，只记录已经发生的美好经历。
 */
@Service
@RequiredArgsConstructor
public class AchievementService {

    private final PetService petService;
    private final TaskMapper taskMapper;
    private final UserInterestMapper interestMapper;
    private final GrowthEventMapper eventMapper;

    public List<GrowthDtos.AchievementDTO> list(Long userId) {
        Pet pet = petService.findPet(userId);
        long completedTasks = taskMapper.selectCount(new LambdaQueryWrapper<Task>()
                .eq(Task::getUserId, userId)
                .eq(Task::getStatus, "completed"));
        long topics = interestMapper.selectCount(new LambdaQueryWrapper<UserInterest>()
                .eq(UserInterest::getUserId, userId));
        long firstChat = eventMapper.selectCount(new LambdaQueryWrapper<GrowthEvent>()
                .eq(GrowthEvent::getUserId, userId)
                .eq(GrowthEvent::getEventType, "first_chat"));
        int activeDays = eventMapper.countActiveDays(userId);
        int level = pet == null ? 0 : pet.getLevel();

        List<GrowthDtos.AchievementDTO> list = new ArrayList<>();
        add(list, "first_meet", "初次相遇", "和伙伴第一次相遇", pet != null, pet == null ? 0 : 1, 1);
        add(list, "first_chat", "第一次聊天", "和伙伴说了第一句话", firstChat > 0, (int) Math.min(firstChat, 1), 1);
        add(list, "first_task", "第一个任务", "完成第一个小任务", completedTasks >= 1, (int) Math.min(completedTasks, 1), 1);
        add(list, "explorer", "探索小达人", "完成 3 个小任务", completedTasks >= 3, (int) Math.min(completedTasks, 3), 3);
        add(list, "dream_sprout", "梦想发芽", "伙伴达到 Lv.3", level >= 3, Math.min(level, 3), 3);
        add(list, "buddy", "默契伙伴", "伙伴达到 Lv.5", level >= 5, Math.min(level, 5), 5);
        add(list, "curious", "好奇心满满", "一起聊过 3 个话题", topics >= 3, (int) Math.min(topics, 3), 3);
        add(list, "steady_days", "细水长流", "累计互动 3 天", activeDays >= 3, Math.min(activeDays, 3), 3);
        return list;
    }

    private void add(List<GrowthDtos.AchievementDTO> list, String key, String title,
                     String description, boolean unlocked, int progress, int target) {
        GrowthDtos.AchievementDTO dto = new GrowthDtos.AchievementDTO();
        dto.setKey(key);
        dto.setTitle(title);
        dto.setDescription(description);
        dto.setUnlocked(unlocked);
        dto.setProgress(Math.min(progress, target));
        dto.setTarget(target);
        list.add(dto);
    }
}