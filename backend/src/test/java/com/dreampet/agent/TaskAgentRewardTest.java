package com.dreampet.agent;

import com.dreampet.config.AppProperties;
import com.dreampet.llm.LlmService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 任务奖励与难度的服务端截断测试。
 *
 * <p>这是「AI 无权决定成长数值」的关键防线：即使模型返回超大奖励，
 * 也必须被夹在 {@code growth.task-reward-min ~ task-reward-max} 区间内。
 */
class TaskAgentRewardTest {

    private LlmService llmService;
    private MemoryManager memoryManager;
    private TaskAgent taskAgent;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        llmService = mock(LlmService.class);
        memoryManager = mock(MemoryManager.class);

        AppProperties props = new AppProperties();
        props.getGrowth().setTaskRewardMin(5);
        props.getGrowth().setTaskRewardMax(30);

        taskAgent = new TaskAgent(llmService, memoryManager, props);
    }

    /** 让 TaskAgent 拿到指定的模型输出 */
    private void givenModelReturns(String json) throws Exception {
        when(llmService.completeCached(anyString())).thenReturn(json);
        when(memoryManager.parseJson(json)).thenReturn(mapper.readTree(json));
    }

    private String draftJson(int reward, int difficulty) {
        return "{\"title\":\"恐龙小侦探\","
                + "\"description\":\"找出三个关于恐龙的有趣发现，讲给家人听\","
                + "\"category\":\"observe\","
                + "\"difficulty\":" + difficulty + ","
                + "\"reward\":" + reward + "}";
    }

    @DisplayName("奖励截断：模型返回值一律被夹在 [5,30]")
    @ParameterizedTest(name = "模型 reward={0} -> 实际 {1}")
    @CsvSource({
            "9999, 30",   // 恶意/幻觉超大值
            "31, 30",     // 略超上限
            "30, 30",     // 上界
            "18, 18",     // 区间内保持原值
            "5, 5",       // 下界
            "4, 5",       // 略低于下限
            "0, 5",       // 零
            "-100, 5"     // 负数
    })
    void rewardIsClampedToConfiguredRange(int modelReward, int expected) throws Exception {
        givenModelReturns(draftJson(modelReward, 1));

        TaskAgent.TaskDraft draft = taskAgent.generate(List.of("恐龙"), List.of(), "seed");

        assertEquals(expected, draft.reward());
    }

    @DisplayName("难度截断：一律被夹在 [1,3]")
    @ParameterizedTest(name = "模型 difficulty={0} -> 实际 {1}")
    @CsvSource({"99, 3", "4, 3", "3, 3", "2, 2", "1, 1", "0, 1", "-5, 1"})
    void difficultyIsClampedToValidRange(int modelDifficulty, int expected) throws Exception {
        givenModelReturns(draftJson(10, modelDifficulty));

        TaskAgent.TaskDraft draft = taskAgent.generate(List.of("恐龙"), List.of(), "seed");

        assertEquals(expected, draft.difficulty());
    }

    @Test
    @DisplayName("非法分类被归一化为 knowledge")
    void invalidCategoryFallsBackToKnowledge() throws Exception {
        String json = "{\"title\":\"测试任务\",\"description\":\"一个测试用的任务描述\","
                + "\"category\":\"hacking\",\"difficulty\":1,\"reward\":10}";
        givenModelReturns(json);

        TaskAgent.TaskDraft draft = taskAgent.generate(List.of(), List.of(), "seed");

        assertEquals("knowledge", draft.category());
    }

    @Test
    @DisplayName("模型输出非 JSON 时使用兜底任务，奖励仍在合法区间")
    void fallbackTaskRewardStaysInRange() {
        when(llmService.completeCached(anyString())).thenReturn("这不是 JSON");
        when(memoryManager.parseJson(anyString())).thenReturn(null);

        TaskAgent.TaskDraft draft = taskAgent.generate(List.of("恐龙"), List.of(), "seed");

        assertFalse(draft.title().isBlank());
        assertTrue(draft.reward() >= 5 && draft.reward() <= 30,
                "兜底任务奖励越界: " + draft.reward());
        assertTrue(draft.difficulty() >= 1 && draft.difficulty() <= 3);
    }

    @Test
    @DisplayName("标题或描述超长时退回兜底任务，不写入异常数据")
    void oversizedDraftFallsBack() throws Exception {
        String longTitle = "恐".repeat(80);
        String json = "{\"title\":\"" + longTitle + "\",\"description\":\"短描述\","
                + "\"category\":\"observe\",\"difficulty\":1,\"reward\":10}";
        givenModelReturns(json);

        TaskAgent.TaskDraft draft = taskAgent.generate(List.of("恐龙"), List.of(), "seed");

        assertTrue(draft.title().length() <= 64);
        assertTrue(draft.reward() >= 5 && draft.reward() <= 30);
    }
}
