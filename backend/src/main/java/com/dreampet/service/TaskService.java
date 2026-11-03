package com.dreampet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dreampet.agent.CompanionAgent;
import com.dreampet.agent.MemoryManager;
import com.dreampet.agent.TaskAgent;
import com.dreampet.common.BizException;
import com.dreampet.dto.TaskDtos;
import com.dreampet.entity.Pet;
import com.dreampet.entity.Task;
import com.dreampet.mapper.TaskMapper;
import com.dreampet.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private static final Set<String> STATUSES = Set.of("pending", "in_progress", "completed");

    private final TaskMapper taskMapper;
    private final PetService petService;
    private final TaskAgent taskAgent;
    private final CompanionAgent companionAgent;
    private final GrowthService growthService;
    private final MemoryManager memoryManager;
    private final RateLimitService rateLimitService;
    private final com.dreampet.config.AppProperties props;

    /** 今日任务（含已完成）+ 更早的未完成任务 */
    public TaskDtos.TodayTasksResponse today(Long userId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        List<Task> tasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
                .eq(Task::getUserId, userId)
                .and(w -> w.ge(Task::getCreatedAt, startOfDay)
                        .or().in(Task::getStatus, "pending", "in_progress"))
                .orderByDesc(Task::getCreatedAt)
                .orderByDesc(Task::getId));
        boolean hasActive = tasks.stream()
                .anyMatch(t -> "pending".equals(t.getStatus()) || "in_progress".equals(t.getStatus()));
        TaskDtos.TodayTasksResponse resp = new TaskDtos.TodayTasksResponse();
        resp.setTasks(tasks.stream().map(this::toDTO).toList());
        resp.setHasActive(hasActive);
        return resp;
    }

    /** 生成任务：已有未完成任务时直接返回，不重复生成 */
    @Transactional
    public TaskDtos.TaskDTO generate(Long userId) {
        Pet pet = requirePet(userId);
        Task active = memoryManager.getActiveTask(userId);
        if (active != null) {
            return toDTO(active);
        }
        rateLimitService.check(userId, "task", props.getRateLimit().getTaskGeneratePerHour());
        List<String> interests = memoryManager.getInterests(userId);
        List<String> recentTitles = taskMapper.selectList(new LambdaQueryWrapper<Task>()
                        .eq(Task::getUserId, userId)
                        .orderByDesc(Task::getCreatedAt)
                        .orderByDesc(Task::getId)
                        .last("LIMIT 10"))
                .stream().map(Task::getTitle).toList();

        TaskAgent.TaskDraft draft = taskAgent.generate(interests, recentTitles, pet.getGrowthStage());
        Task task = new Task();
        task.setUserId(userId);
        task.setPetId(pet.getId());
        task.setTitle(draft.title());
        task.setDescription(draft.description());
        task.setCategory(draft.category());
        task.setDifficulty(draft.difficulty());
        task.setReward(draft.reward());
        task.setStatus("pending");
        taskMapper.insert(task);
        return toDTO(task);
    }

    public TaskDtos.TaskDTO getById(Long userId, Long id) {
        return toDTO(requireTask(userId, id));
    }

    /** 开始任务：pending → in_progress（幂等；已完成的任务保持不变） */
    @Transactional
    public TaskDtos.TaskDTO start(Long userId, Long id) {
        Task task = requireTask(userId, id);
        if ("pending".equals(task.getStatus())) {
            task.setStatus("in_progress");
            taskMapper.updateById(task);
        }
        return toDTO(task);
    }

    public List<TaskDtos.TaskDTO> list(Long userId, String status, int limit) {
        int safeLimit = Math.min(50, Math.max(1, limit));
        LambdaQueryWrapper<Task> query = new LambdaQueryWrapper<Task>()
                .eq(Task::getUserId, userId)
                .eq(status != null && !status.isBlank(), Task::getStatus, status)
                .orderByDesc(Task::getCreatedAt)
                .orderByDesc(Task::getId)
                .last("LIMIT " + safeLimit);
        if (status != null && !status.isBlank() && !STATUSES.contains(status)) {
            return List.of();
        }
        return taskMapper.selectList(query).stream().map(this::toDTO).toList();
    }

    /** 完成任务：条件更新防止重复领经验，反馈失败不影响主流程 */
    @Transactional
    public TaskDtos.CompleteTaskResponse complete(Long userId, Long taskId, String share) {
        Task task = requireTask(userId, taskId);
        if ("completed".equals(task.getStatus())) {
            throw new BizException("这个任务已经完成过啦");
        }
        int updated = taskMapper.completeTaskAtomic(taskId, userId);
        if (updated == 0) {
            throw new BizException("这个任务已经完成过啦");
        }
        task.setStatus("completed");
        task.setCompletedAt(LocalDateTime.now());

        Pet pet = requirePet(userId);
        GrowthService.GrowthResult result = growthService.addExp(
                userId, pet, task.getReward(), "task_completed", "完成任务「" + task.getTitle() + "」");

        String cleanShare = share == null ? "" : share.trim();
        memoryManager.saveEvent(userId, pet.getId(),
                "完成了任务「" + task.getTitle() + "」" + (cleanShare.isEmpty() ? "" : "：" + cleanShare), 3);

        String feedback;
        try {
            feedback = companionAgent.feedback(pet, task, cleanShare);
        } catch (Exception e) {
            log.warn("任务反馈生成失败，使用兜底文案: {}", e.getMessage());
            feedback = "太棒啦！你完成了「" + task.getTitle() + "」！我们都为你感到骄傲，继续加油吧！";
        }

        TaskDtos.CompleteTaskResponse resp = new TaskDtos.CompleteTaskResponse();
        resp.setPet(petService.toDTO(pet, userId));
        resp.setXpGained(result.getExpGained());
        resp.setLevelUp(result.isLevelUp());
        resp.setNewLevel(result.getNewLevel());
        resp.setStageUp(result.isStageUp());
        resp.setNewStage(result.getNewStage());
        resp.setFeedback(feedback);
        return resp;
    }

    private Task requireTask(Long userId, Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null || !userId.equals(task.getUserId())) {
            throw BizException.notFound("任务不存在");
        }
        return task;
    }

    private Pet requirePet(Long userId) {
        Pet pet = petService.findPet(userId);
        if (pet == null) {
            throw BizException.notFound("还没有伙伴");
        }
        return pet;
    }

    public TaskDtos.TaskDTO toDTO(Task task) {
        TaskDtos.TaskDTO dto = new TaskDtos.TaskDTO();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setCategory(task.getCategory());
        dto.setDifficulty(task.getDifficulty());
        dto.setReward(task.getReward());
        dto.setStatus(task.getStatus());
        dto.setCreatedAt(TimeUtil.format(task.getCreatedAt()));
        dto.setCompletedAt(TimeUtil.format(task.getCompletedAt()));
        return dto;
    }

    }