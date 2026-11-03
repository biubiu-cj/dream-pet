package com.dreampet.agent;

import com.dreampet.config.AppProperties;
import com.dreampet.llm.LlmService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 任务生成 Agent：根据兴趣与历史任务生成个性化任务。
 * 输出必须通过结构化校验 + 服务端合理性检查（难度、奖励由后端截断，AI 无最终决定权）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskAgent {

    private static final Set<String> VALID_CATEGORIES =
            Set.of("knowledge", "observe", "creative", "story", "life", "logic");

    private final LlmService llmService;
    private final MemoryManager memoryManager;
    private final AppProperties props;

    public record TaskDraft(String title, String description, String category, int difficulty, int reward) {
    }

    public TaskDraft generate(List<String> interests, List<String> recentTaskTitles, String stage) {
        String prompt = PromptTemplates.taskGen(interests, recentTaskTitles, stage);
        String raw = llmService.completeCached(prompt);
        JsonNode json = memoryManager.parseJson(raw);
        if (json == null) {
            log.warn("任务生成结果非 JSON，使用兜底任务");
            return fallback(interests);
        }

        String title = json.path("title").asText("").trim();
        String desc = json.path("description").asText("").trim();
        String category = json.path("category").asText("knowledge").trim();
        int difficulty = json.path("difficulty").asInt(1);
        int reward = json.path("reward").asInt(10);

        // 结构化校验
        if (title.isEmpty() || title.length() > 64 || desc.isEmpty() || desc.length() > 500) {
            return fallback(interests);
        }
        if (!VALID_CATEGORIES.contains(category)) category = "knowledge";
        // 服务端合理性检查：难度与奖励强制截断在允许区间
        difficulty = Math.max(1, Math.min(3, difficulty));
        reward = Math.max(props.getGrowth().getTaskRewardMin(),
                Math.min(props.getGrowth().getTaskRewardMax(), reward));
        return new TaskDraft(title, desc, category, difficulty, reward);
    }

    private TaskDraft fallback(List<String> interests) {
        String topic = interests.isEmpty() ? "大自然" : interests.get(0);
        return new TaskDraft(
                topic + "小侦探",
                "找出三个和「" + topic + "」有关的有趣小发现，讲给家人或伙伴听",
                "observe", 1, props.getGrowth().getTaskRewardMin() + 5);
    }
}
