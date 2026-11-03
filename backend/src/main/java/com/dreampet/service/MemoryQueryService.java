package com.dreampet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dreampet.agent.MemoryManager;
import com.dreampet.dto.MemoryDtos;
import com.dreampet.entity.Memory;
import com.dreampet.entity.Pet;
import com.dreampet.mapper.MemoryMapper;
import com.dreampet.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** 记忆 / 日记查询：列表与摘要（全部基于真实持久化数据） */
@Service
@RequiredArgsConstructor
public class MemoryQueryService {

    private static final int SUMMARY_EVENTS_LIMIT = 10;
    private static final int PROFILE_LIMIT = 10;

    private final MemoryMapper memoryMapper;
    private final MemoryManager memoryManager;
    private final PetService petService;

    public List<MemoryDtos.MemoryDTO> list(Long userId, String type, int limit) {
        int safeLimit = Math.min(100, Math.max(1, limit));
        List<Memory> list = memoryMapper.selectList(new LambdaQueryWrapper<Memory>()
                .eq(Memory::getUserId, userId)
                .eq(type != null && !type.isBlank(), Memory::getMemoryType, type)
                .orderByDesc(Memory::getCreatedAt)
                .orderByDesc(Memory::getId)
                .last("LIMIT " + safeLimit));
        return list.stream().map(this::toDTO).toList();
    }

    public MemoryDtos.MemorySummary summary(Long userId) {
        Pet pet = petService.findPet(userId);
        MemoryDtos.MemorySummary resp = new MemoryDtos.MemorySummary();
        resp.setSummary(pet == null ? null : memoryManager.getLatestSummary(pet.getId()));
        resp.setImportantEvents(memoryManager.getImportantEvents(userId, SUMMARY_EVENTS_LIMIT));
        resp.setInterests(memoryManager.getInterests(userId));
        resp.setProfile(memoryMapper.selectList(new LambdaQueryWrapper<Memory>()
                        .eq(Memory::getUserId, userId)
                        .eq(Memory::getMemoryType, "profile")
                        .orderByDesc(Memory::getCreatedAt)
                        .last("LIMIT " + PROFILE_LIMIT))
                .stream().map(Memory::getContent).toList());
        return resp;
    }

    private MemoryDtos.MemoryDTO toDTO(Memory m) {
        MemoryDtos.MemoryDTO dto = new MemoryDtos.MemoryDTO();
        dto.setId(m.getId());
        dto.setMemoryType(m.getMemoryType());
        dto.setContent(m.getContent());
        dto.setImportance(m.getImportance());
        dto.setCreatedAt(TimeUtil.format(m.getCreatedAt()));
        return dto;
    }
}