package com.dreampet.agent;

import com.dreampet.entity.ChatMessage;
import com.dreampet.entity.Pet;
import com.dreampet.entity.Task;
import com.dreampet.llm.LlmMessage;
import com.dreampet.llm.LlmService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 伙伴对话 Agent：基于伙伴档案 + 分层记忆生成符合性格的回复。
 * 只负责语言生成，不决定任何数值。
 */
@Component
@RequiredArgsConstructor
public class CompanionAgent {

    private final LlmService llmService;

    public String reply(Pet pet, List<String> interests, String summary,
                        List<String> importantEvents, Task activeTask,
                        List<ChatMessage> recentMessages, String userMessage,
                        Intent intent) {
        String system = PromptTemplates.companion(
                pet.getName(), pet.getSpecies(), pet.getPersonality(),
                interests, summary, importantEvents,
                activeTask == null ? null : activeTask.getTitle() + " - " + activeTask.getDescription(),
                pet.getLevel(), pet.getGrowthStage(), intent);

        List<LlmMessage> history = new ArrayList<>();
        for (ChatMessage m : recentMessages) {
            history.add("user".equals(m.getRole()) ? LlmMessage.user(m.getContent()) : LlmMessage.assistant(m.getContent()));
        }
        history.add(LlmMessage.user(userMessage));
        return llmService.chat(system, history);
    }

    public String feedback(Pet pet, Task task, String childShare) {
        String system = PromptTemplates.feedback(pet.getName(), task.getTitle(), task.getDescription(), childShare);
        return llmService.completeCached(system);
    }

    /** 重新进入时的欢迎语：全部基于持久化的真实记忆数据生成 */
    public String greeting(Pet pet, List<String> interests, String lastTaskTitle,
                           List<String> recentEvents, long daysSinceLastChat) {
        String system = PromptTemplates.greeting(pet.getName(), interests, lastTaskTitle, recentEvents, daysSinceLastChat);
        return llmService.completeCached(system);
    }
}
