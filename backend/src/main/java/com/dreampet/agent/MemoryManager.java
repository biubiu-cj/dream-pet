package com.dreampet.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dreampet.entity.*;
import com.dreampet.llm.LlmService;
import com.dreampet.mapper.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 长期记忆管理器：记忆提取、去重、更新、摘要、上下文组装。
 * 分层记忆：兴趣记忆(user_interests) / 事件记忆(memories) / 任务记忆(tasks)
 *          / 对话摘要(conversation_summaries) / 伙伴档案(pets)。
 * 绝不全量塞聊天记录：只取最近 N 条 + 摘要 + 高重要度事件。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemoryManager {

    private static final int RECENT_MESSAGES_LIMIT = 10;
    private static final int IMPORTANT_EVENTS_LIMIT = 5;
    private static final int INTERESTS_LIMIT = 10;
    private static final int SUMMARIZE_THRESHOLD = 20;

    private final UserInterestMapper interestMapper;
    private final MemoryMapper memoryMapper;
    private final ChatMessageMapper chatMapper;
    private final ConversationSummaryMapper summaryMapper;
    private final TaskMapper taskMapper;
    private final LlmService llmService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ========== 查询 ==========

    public List<String> getInterests(Long userId) {
        return interestMapper.selectList(new LambdaQueryWrapper<UserInterest>()
                        .eq(UserInterest::getUserId, userId)
                        .orderByDesc(UserInterest::getCreatedAt)
                        .last("LIMIT " + INTERESTS_LIMIT))
                .stream().map(UserInterest::getInterest).distinct().toList();
    }

    public List<String> getImportantEvents(Long userId, int limit) {
        return memoryMapper.selectList(new LambdaQueryWrapper<Memory>()
                        .eq(Memory::getUserId, userId)
                        .eq(Memory::getMemoryType, "event")
                        .orderByDesc(Memory::getImportance)
                        .orderByDesc(Memory::getCreatedAt)
                        .last("LIMIT " + limit))
                .stream().map(Memory::getContent).toList();
    }

    public String getLatestSummary(Long petId) {
        List<ConversationSummary> list = summaryMapper.selectList(
                new LambdaQueryWrapper<ConversationSummary>()
                        .eq(ConversationSummary::getPetId, petId)
                        .orderByDesc(ConversationSummary::getCoveredUntil)
                        .last("LIMIT 1"));
        return list.isEmpty() ? null : list.get(0).getSummary();
    }

    public long getLatestCoveredUntil(Long petId) {
        List<ConversationSummary> list = summaryMapper.selectList(
                new LambdaQueryWrapper<ConversationSummary>()
                        .eq(ConversationSummary::getPetId, petId)
                        .orderByDesc(ConversationSummary::getCoveredUntil)
                        .last("LIMIT 1"));
        return list.isEmpty() ? 0 : list.get(0).getCoveredUntil();
    }

    public List<ChatMessage> getRecentMessages(Long petId, int limit) {
        List<ChatMessage> desc = chatMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getPetId, petId)
                .in(ChatMessage::getRole, "user", "assistant")
                .orderByDesc(ChatMessage::getId)
                .last("LIMIT " + limit));
        List<ChatMessage> asc = new ArrayList<>(desc);
        java.util.Collections.reverse(asc);
        return asc;
    }

    public Task getActiveTask(Long userId) {
        List<Task> list = taskMapper.selectList(new LambdaQueryWrapper<Task>()
                .eq(Task::getUserId, userId)
                .in(Task::getStatus, "pending", "in_progress")
                .orderByDesc(Task::getCreatedAt)
                .last("LIMIT 1"));
        return list.isEmpty() ? null : list.get(0);
    }

    public LocalDateTime getLastChatTime(Long petId) {
        List<ChatMessage> list = chatMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getPetId, petId)
                .eq(ChatMessage::getRole, "user")
                .orderByDesc(ChatMessage::getId)
                .last("LIMIT 1"));
        return list.isEmpty() ? null : list.get(0).getCreatedAt();
    }

    // ========== 写入（带去重） ==========

    /** 保存兴趣，返回是否为新兴趣 */
    public boolean saveInterest(Long userId, String interest, String source) {
        String clean = interest.trim();
        if (clean.isEmpty() || clean.length() > 32) return false;
        Long exists = interestMapper.selectCount(new LambdaQueryWrapper<UserInterest>()
                .eq(UserInterest::getUserId, userId)
                .eq(UserInterest::getInterest, clean));
        if (exists > 0) return false;
        UserInterest ui = new UserInterest();
        ui.setUserId(userId);
        ui.setInterest(clean);
        ui.setSource(source);
        interestMapper.insert(ui);
        return true;
    }

    /** 保存事件记忆，7 天内相同内容去重 */
    public boolean saveEvent(Long userId, Long petId, String content, int importance) {
        String clean = content.trim();
        if (clean.isEmpty() || clean.length() > 500) return false;
        Long dup = memoryMapper.selectCount(new LambdaQueryWrapper<Memory>()
                .eq(Memory::getUserId, userId)
                .eq(Memory::getMemoryType, "event")
                .eq(Memory::getContent, clean)
                .ge(Memory::getCreatedAt, LocalDateTime.now().minusDays(7)));
        if (dup > 0) return false;
        Memory m = new Memory();
        m.setUserId(userId);
        m.setPetId(petId);
        m.setMemoryType("event");
        m.setContent(clean);
        m.setImportance(Math.max(1, Math.min(5, importance)));
        memoryMapper.insert(m);
        return true;
    }

    /** 保存画像记忆（去重） */
    public boolean saveProfile(Long userId, Long petId, String content) {
        String clean = content.trim();
        if (clean.isEmpty() || clean.length() > 200) return false;
        Long dup = memoryMapper.selectCount(new LambdaQueryWrapper<Memory>()
                .eq(Memory::getUserId, userId)
                .eq(Memory::getMemoryType, "profile")
                .eq(Memory::getContent, clean));
        if (dup > 0) return false;
        Memory m = new Memory();
        m.setUserId(userId);
        m.setPetId(petId);
        m.setMemoryType("profile");
        m.setContent(clean);
        m.setImportance(3);
        memoryMapper.insert(m);
        return true;
    }

    public void saveChatMessage(Long userId, Long petId, String role, String content) {
        ChatMessage m = new ChatMessage();
        m.setUserId(userId);
        m.setPetId(petId);
        m.setRole(role);
        m.setContent(content.length() > 2000 ? content.substring(0, 2000) : content);
        chatMapper.insert(m);
    }

    // ========== 记忆提取（LLM / Mock 规则） ==========

    /**
     * 从一轮对话中提取并保存兴趣、事件、画像。
     * 由 ChatService 异步调用，不阻塞聊天响应。
     */
    public void extractAndSave(Long userId, Long petId, String userMessage, String assistantReply) {
        try {
            List<String> existing = getInterests(userId);
            String prompt = PromptTemplates.memoryExtract(existing, userMessage, assistantReply);
            String result = llmService.complete(prompt);
            JsonNode json = parseJson(result);
            if (json == null) return;

            if (json.has("interests")) {
                for (JsonNode n : json.get("interests")) {
                    if (saveInterest(userId, n.asText(), "chat")) {
                        saveEvent(userId, petId, "孩子表达了对「" + n.asText() + "」的兴趣", 2);
                    }
                }
            }
            if (json.has("events")) {
                for (JsonNode n : json.get("events")) {
                    saveEvent(userId, petId, n.asText(), 3);
                }
            }
            if (json.has("profile") && !json.get("profile").asText().isBlank()) {
                saveProfile(userId, petId, "孩子" + json.get("profile").asText());
            }
        } catch (Exception e) {
            // 记忆提取失败不影响主流程
            log.warn("记忆提取失败: {}", e.getMessage());
        }
    }

    // ========== 对话摘要 ==========

    /** 未摘要消息超过阈值时，滚动生成新摘要（LLM / Mock 抽取式） */
    public void maybeSummarize(Long userId, Long petId, String petName) {
        try {
            long covered = getLatestCoveredUntil(petId);
            List<ChatMessage> uncovered = chatMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                    .eq(ChatMessage::getPetId, petId)
                    .in(ChatMessage::getRole, "user", "assistant")
                    .gt(ChatMessage::getId, covered)
                    .orderByAsc(ChatMessage::getId));
            if (uncovered.size() < SUMMARIZE_THRESHOLD) return;

            List<ChatMessage> toCover = uncovered.subList(0, Math.min(uncovered.size(), 40));
            List<String[]> dialogues = toCover.stream()
                    .map(m -> new String[]{"孩子".equals(roleLabel(m.getRole())) ? "孩子" : petName, m.getContent()})
                    .toList();
            String existing = getLatestSummary(petId);
            String prompt = PromptTemplates.summary(petName, existing, dialogues);
            String newSummary = llmService.complete(prompt);
            if (newSummary.length() > 1000) newSummary = newSummary.substring(0, 1000);

            ConversationSummary s = new ConversationSummary();
            s.setUserId(userId);
            s.setPetId(petId);
            s.setSummary(newSummary);
            s.setCoveredUntil(toCover.get(toCover.size() - 1).getId());
            summaryMapper.insert(s);
        } catch (Exception e) {
            log.warn("对话摘要失败: {}", e.getMessage());
        }
    }

    private String roleLabel(String role) {
        return "user".equals(role) ? "孩子" : "伙伴";
    }

    /** 宽容解析：从文本中提取第一个 JSON 对象 */
    public JsonNode parseJson(String text) {
        if (text == null) return null;
        String t = text.trim().replaceAll("```json", "").replaceAll("```", "").trim();
        int start = t.indexOf('{');
        int end = t.lastIndexOf('}');
        if (start < 0 || end <= start) return null;
        try {
            return objectMapper.readTree(t.substring(start, end + 1));
        } catch (Exception e) {
            return null;
        }
    }

    public int recentMessagesLimit() {
        return RECENT_MESSAGES_LIMIT;
    }

    public int importantEventsLimit() {
        return IMPORTANT_EVENTS_LIMIT;
    }
}
