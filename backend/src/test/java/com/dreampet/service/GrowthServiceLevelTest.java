package com.dreampet.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 成长数值的纯函数单元测试（不加载 Spring 上下文）。
 *
 * <p>覆盖等级阈值边界、满级封顶、阶段切换边界、进度条取值范围。
 * 阈值定义见 {@link GrowthService}：Lv1=0, Lv2=30, Lv3=80, Lv4=150, Lv5=250,
 * Lv6=380, Lv7=550, Lv8=760, Lv9=1020, Lv10=1350
 */
class GrowthServiceLevelTest {

    @DisplayName("等级阈值边界：差 1 点经验不应升级，刚好到达则升级")
    @ParameterizedTest(name = "exp={0} -> Lv{1}")
    @CsvSource({
            "0, 1", "1, 1", "29, 1",
            "30, 2", "31, 2", "79, 2",
            "80, 3", "149, 3",
            "150, 4", "249, 4",
            "250, 5", "379, 5",
            "380, 6", "549, 6",
            "550, 7", "759, 7",
            "760, 8", "1019, 8",
            "1020, 9", "1349, 9",
            "1350, 10"
    })
    void levelForExpRespectsThresholds(int exp, int expectedLevel) {
        assertEquals(expectedLevel, GrowthService.levelForExp(exp));
    }

    @Test
    @DisplayName("满级封顶：超过最高阈值仍为 Lv10，且不再提示所需经验")
    void levelIsCappedAtMaxLevel() {
        assertEquals(GrowthService.MAX_LEVEL, GrowthService.levelForExp(1350));
        assertEquals(GrowthService.MAX_LEVEL, GrowthService.levelForExp(99_999));
        assertEquals(0, GrowthService.expToNextLevel(1350));
        assertEquals(0, GrowthService.expToNextLevel(99_999));
        assertEquals(100, GrowthService.levelProgress(1350));
        assertEquals(100, GrowthService.levelProgress(99_999));
    }

    @DisplayName("成长阶段边界：seed(1~2) / sprout(3~4) / bloom(5~7) / dream(8~10)")
    @ParameterizedTest(name = "Lv{0} -> {1}")
    @CsvSource({
            "1, seed", "2, seed",
            "3, sprout", "4, sprout",
            "5, bloom", "6, bloom", "7, bloom",
            "8, dream", "9, dream", "10, dream"
    })
    void stageForLevelBoundaries(int level, String expectedStage) {
        assertEquals(expectedStage, GrowthService.stageForLevel(level));
    }

    @Test
    @DisplayName("距下一级经验：等于下一阈值与当前经验之差")
    void expToNextLevelIsDifferenceToNextThreshold() {
        assertEquals(30, GrowthService.expToNextLevel(0));
        assertEquals(1, GrowthService.expToNextLevel(29));
        assertEquals(50, GrowthService.expToNextLevel(30));
        assertEquals(1, GrowthService.expToNextLevel(79));
    }

    @Test
    @DisplayName("进度条：始终落在 0~100，升级瞬间归零")
    void levelProgressStaysInRange() {
        for (int exp = 0; exp <= 1400; exp++) {
            int progress = GrowthService.levelProgress(exp);
            assertTrue(progress >= 0 && progress <= 100,
                    "exp=" + exp + " 的进度越界: " + progress);
        }
        // 刚好升级时，新等级内进度从 0 开始
        assertEquals(0, GrowthService.levelProgress(30));
        assertEquals(0, GrowthService.levelProgress(80));
        assertEquals(0, GrowthService.levelProgress(250));
    }

    @Test
    @DisplayName("阶段标签与阶段 key 一一对应")
    void stageLabelMapping() {
        assertEquals("初识伙伴", GrowthService.stageLabel("seed"));
        assertEquals("好奇伙伴", GrowthService.stageLabel("sprout"));
        assertEquals("默契伙伴", GrowthService.stageLabel("bloom"));
        assertEquals("梦想伙伴", GrowthService.stageLabel("dream"));
        // 未知 key 退化为初始阶段，不抛异常
        assertEquals("初识伙伴", GrowthService.stageLabel("unknown"));
    }
}
