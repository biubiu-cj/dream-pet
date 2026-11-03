package com.dreampet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dreampet.agent.CompanionAgent;
import com.dreampet.agent.Intent;
import com.dreampet.agent.IntentRecognizer;
import com.dreampet.agent.MemoryManager;
import com.dreampet.agent.SafetyGuard;
import com.dreampet.common.BizException;
import com.dreampet.config.AppProperties;
import com.dreampet.dto.ChatDtos;
import com.dreampet.dto.TaskDtos;
import com.dreampet.entity.ChatMessage;
import com.dreampet.entity.Pet;
import com.dreampet.entity.Task;
import com.dreampet.mapper.TaskMapper;
import com.dreampet.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final PetService petService;
    private final TaskService taskService;
    private final TaskMapper taskMapper;
    private final MemoryManager memoryManager;
    private final SafetyGuard safetyGuard;
    private final IntentRecognizer intentRecognizer;
    private final CompanionAgent companionAgent;
    private final ChatPostProcessor chatPostProcessor;
    private final RateLimitService rateLimitService;
    private final AppProperties props;

    public ChatDtos.ChatResponse chat(Long userId, String rawMessage) {
        Pet pet = petService.findPet(userId);
        if (pet == null) {
            throw BizException.notFound("还没有伙伴");
        }
        String message = rawMessage == null ? "" : rawMessage.trim();
        rateLimitService.check(userId, "chat", props.getRateLimit().getChatPerHour());
        Task activeTask = memoryManager.getActiveTask(userId);

        // 1. 输入安全拦截：仍保存对话，便于家长回顾
        Optional<String> blocked = safetyGuard.checkInput(message);
        if (blocked.isPresent()) {
            memoryManager.saveChatMessage(userId, pet.getId(), "user", message);
            memoryManager.saveChatMessage(userId, pet.getId(), "assistant", blocked.get());
            return buildResponse(blocked.get(), pet, userId, null, activeTask == null, null);
        }

        // 2. 用户意图识别（规则优先，必要时真实模型兜底）
        Intent intent = intentRecognizer.recognize(message);

        // 3. 组装分层记忆上下文（最近 N 条 + 摘要 + 与当前话题相关的高重要度事件）
        List<String> interests = memoryManager.getInterests(userId);
        String summary = memoryManager.getLatestSummary(pet.getId());
        List<String> events = memoryManager.getRelevantEvents(
                userId, memoryManager.importantEventsLimit(), message);
        List<ChatMessage> recent = memoryManager.getRecentMessages(pet.getId(), memoryManager.recentMessagesLimit());

        // 4. 生成回复并做输出安全过滤
        String reply = companionAgent.reply(pet, interests, summary, events, activeTask, recent, message, intent);
        reply = safetyGuard.sanitizeOutput(reply, pet.getName());

        // 5. 保存两条消息，再异步做记忆提取 / 摘要 / 经验结算
        memoryManager.saveChatMessage(userId, pet.getId(), "user", message);
        memoryManager.saveChatMessage(userId, pet.getId(), "assistant", reply);
        chatPostProcessor.process(userId, pet.getId(), pet.getName(), message, reply);

        // 6. 意图为「想要任务」时，按真实兴趣生成
        TaskDtos.TaskDTO suggested = null;
        if (activeTask == null && intent == Intent.TASK_REQUEST) {
            try {
                suggested = taskService.generate(userId);
            } catch (Exception e) {
                log.warn("任务推荐失败: {}", e.getMessage());
            }
        }
        boolean canSuggestTask = activeTask == null && suggested == null;
        return buildResponse(reply, pet, userId, suggested, canSuggestTask, intent);
    }

    public ChatDtos.HistoryResponse history(Long userId, int limit, boolean greeting) {
        int safeLimit = Math.min(100, Math.max(1, limit));
        Pet pet = petService.findPet(userId);
        if (pet == null) {
            ChatDtos.HistoryResponse empty = new ChatDtos.HistoryResponse();
            empty.setMessages(List.of());
            empty.setGreeting(null);
            return empty;
        }
        List<ChatMessage> messages = memoryManager.getRecentMessages(pet.getId(), safeLimit);
        String greetingText = null;

        if (greeting) {
            LocalDateTime lastUserTime = memoryManager.getLastChatTime(pet.getId());
            // 10 分钟内已生成过欢迎语则不再重复（防止反复刷新页面刷屏）；
            // 更久之后重新回来仍会生成新的欢迎语。
            // 依据结构化 message_type 判断，不依赖 AI 返回文本的前缀。
            LocalDateTime lastGreetingTime = memoryManager.getLastGreetingTime(pet.getId());
            boolean recentlyGreeted = lastGreetingTime != null
                    && lastGreetingTime.isAfter(LocalDateTime.now().minusMinutes(10));
            if (lastUserTime != null
                    && !recentlyGreeted
                    && lastUserTime.isBefore(LocalDateTime.now()
                    .minusMinutes(props.getChat().getGreetingIdleMinutes()))) {
                try {
                    Task lastTask = lastCompletedTask(userId);
                    long days = Duration.between(lastUserTime, LocalDateTime.now()).toDays();
                    greetingText = companionAgent.greeting(
                            pet,
                            memoryManager.getInterests(userId),
                            lastTask == null ? null : lastTask.getTitle(),
                            memoryManager.getImportantEvents(userId, 5),
                            days);
                    memoryManager.saveChatMessage(userId, pet.getId(), "assistant", greetingText,
                            ChatMessage.TYPE_GREETING);
                    messages = memoryManager.getRecentMessages(pet.getId(), safeLimit);
                } catch (Exception e) {
                    log.warn("欢迎语生成失败: {}", e.getMessage());
                    greetingText = null;
                }
            }
        }

        ChatDtos.HistoryResponse resp = new ChatDtos.HistoryResponse();
        resp.setMessages(messages.stream().map(this::toMessageDTO).toList());
        resp.setGreeting(greetingText);
        return resp;
    }

    /**
     * 开启新对话：写入 session_start 分隔标记，清空当前聊天窗口的上下文。
     * 只影响聊天窗口；长期记忆、成长记录、历史消息仍保留在数据库与成长日记中。
     */
    public ChatDtos.HistoryResponse startNewConversation(Long userId) {
        Pet pet = petService.findPet(userId);
        if (pet == null) {
            throw BizException.notFound("还没有伙伴");
        }
        memoryManager.saveChatMessage(userId, pet.getId(), "system", "新对话开始",
                ChatMessage.TYPE_SESSION_START);
        String opener = "我是" + pet.getName() + "！我们开始新的对话吧，今天想聊点什么？";
        memoryManager.saveChatMessage(userId, pet.getId(), "assistant", opener);

        ChatDtos.HistoryResponse resp = new ChatDtos.HistoryResponse();
        resp.setMessages(memoryManager.getRecentMessages(pet.getId(), 50)
                .stream().map(this::toMessageDTO).toList());
        resp.setGreeting(null);
        return resp;
    }

    private Task lastCompletedTask(Long userId) {
        List<Task> list = taskMapper.selectList(new LambdaQueryWrapper<Task>()
                .eq(Task::getUserId, userId)
                .eq(Task::getStatus, "completed")
                .orderByDesc(Task::getCompletedAt)
                .orderByDesc(Task::getId)
                .last("LIMIT 1"));
        return list.isEmpty() ? null : list.get(0);
    }

    private ChatDtos.ChatResponse buildResponse(String reply, Pet pet, Long userId,
                                                TaskDtos.TaskDTO suggested, boolean canSuggestTask,
                                                Intent intent) {
        ChatDtos.ChatResponse resp = new ChatDtos.ChatResponse();
        resp.setReply(reply);
        resp.setPet(petService.toDTO(pet, userId));
        resp.setSuggestedTask(suggested);
        resp.setCanSuggestTask(canSuggestTask);
        resp.setMood(resp.getPet().getMood());
        // 运行时只使用真实模型；demo 字段保留兼容前端，固定为 false
        resp.setDemo(false);
        if (intent == null) {
            resp.setIntent("safety");
            resp.setIntentLabel("安全引导");
        } else {
            resp.setIntent(intent.code());
            resp.setIntentLabel(intent.getLabel());
        }
        return resp;
    }

    private ChatDtos.ChatMessageDTO toMessageDTO(ChatMessage m) {
        ChatDtos.ChatMessageDTO dto = new ChatDtos.ChatMessageDTO();
        dto.setId(m.getId());
        dto.setRole(m.getRole());
        dto.setContent(m.getContent());
        dto.setCreatedAt(TimeUtil.format(m.getCreatedAt()));
        return dto;
    }
}