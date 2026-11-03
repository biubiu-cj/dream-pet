package com.dreampet.agent;

import com.dreampet.entity.Memory;
import com.dreampet.mapper.*;
import com.dreampet.llm.LlmService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 记忆检索的话题相关性重排测试。
 *
 * <p>原实现仅按「重要度 + 时间」召回，孩子聊恐龙时可能取出上周关于画画的高重要度记忆。
 * {@code getRelevantEvents} 在同一候选集内按关键词重合度重排，且在无相关项时
 * 退化为原有排序，保证行为兼容。
 */
class MemoryManagerRelevanceTest {

    private MemoryMapper memoryMapper;
    private MemoryManager memoryManager;

    @BeforeEach
    void setUp() {
        memoryMapper = mock(MemoryMapper.class);
        memoryManager = new MemoryManager(
                mock(UserInterestMapper.class),
                memoryMapper,
                mock(ChatMessageMapper.class),
                mock(ConversationSummaryMapper.class),
                mock(TaskMapper.class),
                mock(LlmService.class));
    }

    private Memory mem(String content, int importance, int daysAgo) {
        Memory m = new Memory();
        m.setContent(content);
        m.setImportance(importance);
        m.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
        return m;
    }

    /** 候选池查询返回给定列表（模拟数据库已按重要度+时间排好序） */
    private void givenCandidates(List<Memory> candidates) {
        when(memoryMapper.selectList(any())).thenReturn(new ArrayList<>(candidates));
    }

    @Test
    @DisplayName("与当前话题相关的记忆被提前，即使重要度较低")
    void relevantMemoryIsPrioritized() {
        givenCandidates(List.of(
                mem("孩子画了一幅水彩画，很认真", 5, 7),
                mem("孩子说长大想当画家", 5, 6),
                mem("孩子分享了霸王龙的爪子很大", 2, 3),
                mem("孩子今天吃了面条", 1, 1)
        ));

        List<String> result = memoryManager.getRelevantEvents(1L, 2, "我们再聊聊霸王龙好不好");

        assertEquals(2, result.size());
        assertTrue(result.get(0).contains("霸王龙"),
                "与当前消息相关的记忆应排在首位，实际: " + result);
    }

    @Test
    @DisplayName("没有相关记忆时退化为原有的重要度优先排序")
    void fallsBackToImportanceOrderWhenNothingRelevant() {
        givenCandidates(List.of(
                mem("孩子说长大想当画家", 5, 6),
                mem("孩子画了水彩画", 4, 5),
                mem("孩子今天吃了面条", 1, 1),
                mem("孩子看了动画片", 1, 2)
        ));

        List<String> result = memoryManager.getRelevantEvents(1L, 2, "你好呀");

        assertEquals(2, result.size());
        assertTrue(result.get(0).contains("画家"),
                "无相关项时应保持重要度优先，实际: " + result);
    }

    @Test
    @DisplayName("当前消息为空时行为与 getImportantEvents 一致")
    void blankMessageKeepsOriginalBehaviour() {
        givenCandidates(List.of(
                mem("高重要度记忆", 5, 1),
                mem("低重要度记忆", 1, 1)
        ));

        List<String> withNull = memoryManager.getRelevantEvents(1L, 5, null);
        List<String> withBlank = memoryManager.getRelevantEvents(1L, 5, "   ");

        assertEquals(List.of("高重要度记忆", "低重要度记忆"), withNull);
        assertEquals(withNull, withBlank);
    }

    @Test
    @DisplayName("候选数不超过 limit 时按原顺序全量返回")
    void returnsAllWhenCandidatesFitLimit() {
        givenCandidates(List.of(
                mem("记忆A", 3, 1),
                mem("记忆B", 2, 2)
        ));

        List<String> result = memoryManager.getRelevantEvents(1L, 5, "任意消息");

        assertEquals(List.of("记忆A", "记忆B"), result);
    }

    @Test
    @DisplayName("importance 为 null 的历史数据不会导致异常")
    void handlesNullImportanceSafely() {
        Memory legacy = new Memory();
        legacy.setContent("早期数据没有重要度");
        legacy.setImportance(null);
        legacy.setCreatedAt(LocalDateTime.now().minusDays(2));

        givenCandidates(List.of(
                legacy,
                mem("孩子喜欢恐龙", 3, 1),
                mem("孩子喜欢画画", 2, 1)
        ));

        assertDoesNotThrow(() -> {
            List<String> result = memoryManager.getRelevantEvents(1L, 2, "恐龙好厉害");
            assertEquals(2, result.size());
            assertTrue(result.get(0).contains("恐龙"));
        });
    }

    @Test
    @DisplayName("createdAt 为 null 时排序不抛异常")
    void handlesNullCreatedAtSafely() {
        Memory noTime = new Memory();
        noTime.setContent("没有时间戳的记忆");
        noTime.setImportance(2);
        noTime.setCreatedAt(null);

        givenCandidates(List.of(
                noTime,
                mem("孩子喜欢恐龙", 2, 1),
                mem("孩子喜欢画画", 2, 2)
        ));

        assertDoesNotThrow(() -> memoryManager.getRelevantEvents(1L, 2, "不相关的话题"));
    }
}
