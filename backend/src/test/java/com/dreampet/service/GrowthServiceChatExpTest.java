package com.dreampet.service;

import com.dreampet.entity.Pet;
import com.dreampet.mapper.GrowthEventMapper;
import com.dreampet.mapper.PetMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 聊天经验的每日上限截断测试。
 *
 * <p>对应 {@link GrowthService#addChatExp}：单次获得的经验不得超过
 * 「每日上限 - 今日已获得」，且不产生负数。
 */
class GrowthServiceChatExpTest {

    private GrowthEventMapper eventMapper;
    private PetMapper petMapper;
    private GrowthService growthService;

    @BeforeEach
    void setUp() {
        eventMapper = mock(GrowthEventMapper.class);
        petMapper = mock(PetMapper.class);
        growthService = new GrowthService(petMapper, eventMapper);
    }

    private Pet newPet() {
        Pet pet = new Pet();
        pet.setId(1L);
        pet.setLevel(1);
        pet.setExperience(0);
        pet.setGrowthStage("seed");
        return pet;
    }

    @Test
    @DisplayName("今日未达上限：正常获得单次聊天经验")
    void grantsFullExpWhenUnderCap() {
        when(eventMapper.sumTodayChatExp(1L)).thenReturn(0);
        Pet pet = newPet();

        GrowthService.GrowthResult result = growthService.addChatExp(1L, pet, 2, 20);

        assertEquals(2, result.getExpGained());
        assertEquals(2, pet.getExperience());
    }

    @Test
    @DisplayName("接近上限：本次经验被截断到剩余额度")
    void truncatesExpToRemainingQuota() {
        // 今日已获得 19，上限 20，本次最多只能再拿 1
        when(eventMapper.sumTodayChatExp(1L)).thenReturn(19);
        Pet pet = newPet();

        GrowthService.GrowthResult result = growthService.addChatExp(1L, pet, 2, 20);

        assertEquals(1, result.getExpGained(), "超出每日上限的部分必须被截断");
        assertEquals(1, pet.getExperience());
    }

    @Test
    @DisplayName("已达上限：不再获得经验，且不写入成长事件")
    void grantsNothingWhenCapReached() {
        when(eventMapper.sumTodayChatExp(1L)).thenReturn(20);
        Pet pet = newPet();

        GrowthService.GrowthResult result = growthService.addChatExp(1L, pet, 2, 20);

        assertEquals(0, result.getExpGained());
        assertEquals(0, pet.getExperience());
        assertEquals(pet.getLevel(), result.getNewLevel());
        verify(eventMapper, never()).insert(any(com.dreampet.entity.GrowthEvent.class));
        verify(petMapper, never()).updateById(any(Pet.class));
    }

    @Test
    @DisplayName("已超出上限：不产生负经验")
    void neverGrantsNegativeExp() {
        // 历史数据异常导致今日已超上限
        when(eventMapper.sumTodayChatExp(1L)).thenReturn(50);
        Pet pet = newPet();

        GrowthService.GrowthResult result = growthService.addChatExp(1L, pet, 2, 20);

        assertEquals(0, result.getExpGained(), "经验不得为负");
        assertEquals(0, pet.getExperience());
    }

    @Test
    @DisplayName("上限为 0：聊天不再产生经验")
    void zeroCapGrantsNothing() {
        when(eventMapper.sumTodayChatExp(1L)).thenReturn(0);
        Pet pet = newPet();

        GrowthService.GrowthResult result = growthService.addChatExp(1L, pet, 2, 0);

        assertEquals(0, result.getExpGained());
        verify(petMapper, never()).updateById(any(Pet.class));
    }

    @Test
    @DisplayName("跨越阈值时正确标记升级与阶段变化")
    void marksLevelUpAndStageUp() {
        when(eventMapper.sumTodayChatExp(1L)).thenReturn(0);
        Pet pet = newPet();
        pet.setExperience(79);   // 再得 1 点即到 80 → Lv3 → sprout
        pet.setLevel(2);
        pet.setGrowthStage("seed");

        GrowthService.GrowthResult result = growthService.addChatExp(1L, pet, 2, 20);

        assertEquals(81, pet.getExperience());
        assertEquals(3, result.getNewLevel());
        assertTrue(result.isLevelUp());
        assertTrue(result.isStageUp());
        assertEquals("sprout", result.getNewStage());
    }
}
