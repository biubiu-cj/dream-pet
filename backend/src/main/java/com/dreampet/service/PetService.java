package com.dreampet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dreampet.agent.MemoryManager;
import com.dreampet.agent.PromptTemplates;
import com.dreampet.common.BizException;
import com.dreampet.config.AppProperties;
import com.dreampet.dto.PetDtos;
import com.dreampet.entity.Pet;
import com.dreampet.entity.UserInterest;
import com.dreampet.mapper.PetMapper;
import com.dreampet.mapper.UserInterestMapper;
import com.dreampet.util.TimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 伙伴服务：创建、查询、更新与 DTO 组装。
 * 所有成长数值由后端计算，AI 不参与。
 */
@Service
@RequiredArgsConstructor
public class PetService {

    private static final Set<String> SPECIES =
            Set.of("fox", "rabbit", "cat", "bear", "dino");
    private static final Set<String> PERSONALITIES =
            Set.of("curious", "gentle", "brave", "clever", "calm");

    private final PetMapper petMapper;
    private final UserInterestMapper interestMapper;
    private final GrowthService growthService;
    private final MemoryManager memoryManager;
    private final AppProperties props;

    @Transactional
    public PetDtos.PetDTO create(Long userId, PetDtos.CreatePetRequest req) {
        Long exists = petMapper.selectCount(new LambdaQueryWrapper<Pet>().eq(Pet::getUserId, userId));
        if (exists > 0) {
            throw new BizException("每个人只能有一个伙伴哦");
        }
        String name = req.getName() == null ? "" : req.getName().trim();
        if (name.isEmpty() || name.length() > 16) {
            throw new BizException("伙伴名字需要 1~16 个字");
        }
        if (!SPECIES.contains(req.getSpecies())) {
            throw new BizException("请选择一种小伙伴");
        }
        if (!PERSONALITIES.contains(req.getPersonality())) {
            throw new BizException("请选择一种性格");
        }
        List<String> interests = cleanInterests(req.getInterests());

        Pet pet = new Pet();
        pet.setUserId(userId);
        pet.setName(name);
        pet.setSpecies(req.getSpecies());
        pet.setPersonality(req.getPersonality());
        pet.setLevel(1);
        pet.setExperience(0);
        pet.setGrowthStage("seed");
        pet.setAvatarKey(req.getSpecies());
        petMapper.insert(pet);

        for (String interest : interests) {
            memoryManager.saveInterest(userId, interest, "create");
        }
        int exp = props.getGrowth().getCreatePetExp();
        if (exp > 0) {
            growthService.addExp(userId, pet, exp, "pet_created", "和伙伴「" + name + "」相遇啦");
        } else {
            growthService.recordEvent(userId, pet.getId(), "pet_created", "和伙伴「" + name + "」相遇啦", 0);
        }
        return toDTO(pet, userId);
    }

    public PetDtos.PetDTO current(Long userId) {
        Pet pet = findPet(userId);
        if (pet == null) {
            throw BizException.notFound("还没有伙伴");
        }
        return toDTO(pet, userId);
    }

    public PetDtos.PetDTO getById(Long userId, Long id) {
        Pet pet = petMapper.selectById(id);
        if (pet == null || !userId.equals(pet.getUserId())) {
            throw BizException.notFound("伙伴不存在");
        }
        return toDTO(pet, userId);
    }

    @Transactional
    public PetDtos.PetDTO update(Long userId, Long id, PetDtos.UpdatePetRequest req) {
        Pet pet = petMapper.selectById(id);
        if (pet == null || !userId.equals(pet.getUserId())) {
            throw BizException.notFound("伙伴不存在");
        }
        if (req.getName() != null) {
            String name = req.getName().trim();
            if (name.isEmpty() || name.length() > 16) {
                throw new BizException("伙伴名字需要 1~16 个字");
            }
            pet.setName(name);
        }
        if (req.getSpecies() != null) {
            if (!SPECIES.contains(req.getSpecies())) {
                throw new BizException("请选择一种小伙伴");
            }
            pet.setSpecies(req.getSpecies());
            pet.setAvatarKey(req.getSpecies());
        }
        if (req.getPersonality() != null) {
            if (!PERSONALITIES.contains(req.getPersonality())) {
                throw new BizException("请选择一种性格");
            }
            pet.setPersonality(req.getPersonality());
        }
        if (req.getInterests() != null) {
            List<String> interests = cleanInterests(req.getInterests());
            LambdaQueryWrapper<UserInterest> remove = new LambdaQueryWrapper<UserInterest>()
                    .eq(UserInterest::getUserId, userId)
                    .eq(UserInterest::getSource, "create");
            if (!interests.isEmpty()) {
                remove.notIn(UserInterest::getInterest, interests);
            }
            interestMapper.delete(remove);
            for (String interest : interests) {
                memoryManager.saveInterest(userId, interest, "create");
            }
        }
        petMapper.updateById(pet);
        return toDTO(pet, userId);
    }

    public Pet findPet(Long userId) {
        List<Pet> list = petMapper.selectList(new LambdaQueryWrapper<Pet>()
                .eq(Pet::getUserId, userId)
                .last("LIMIT 1"));
        return list.isEmpty() ? null : list.get(0);
    }

    /** 组装 PetDTO：名称映射、经验进度、兴趣、心情 */
    public PetDtos.PetDTO toDTO(Pet pet, Long userId) {
        PetDtos.PetDTO dto = new PetDtos.PetDTO();
        dto.setId(pet.getId());
        dto.setName(pet.getName());
        dto.setSpecies(pet.getSpecies());
        dto.setSpeciesName(PromptTemplates.speciesName(pet.getSpecies()));
        dto.setPersonality(pet.getPersonality());
        dto.setPersonalityName(PromptTemplates.personalityName(pet.getPersonality()));
        dto.setLevel(pet.getLevel());
        dto.setExperience(pet.getExperience());
        dto.setGrowthStage(pet.getGrowthStage());
        dto.setStageName(PromptTemplates.stageName(pet.getGrowthStage()));
        dto.setAvatarKey(pet.getAvatarKey());
        dto.setExpToNextLevel(GrowthService.expToNextLevel(pet.getExperience()));
        dto.setLevelProgress(GrowthService.levelProgress(pet.getExperience()));
        dto.setInterests(memoryManager.getInterests(userId));
        dto.setMood(moodOf(pet));
        dto.setCreatedAt(TimeUtil.format(pet.getCreatedAt()));
        return dto;
    }

    /** 心情：从未聊天 excited；<2h excited、<24h happy、<72h calm、否则 sleepy */
    public String moodOf(Pet pet) {
        LocalDateTime last = memoryManager.getLastChatTime(pet.getId());
        if (last == null) {
            return "excited";
        }
        long minutes = Duration.between(last, LocalDateTime.now()).toMinutes();
        if (minutes < 120) {
            return "excited";
        }
        if (minutes < 1440) {
            return "happy";
        }
        if (minutes < 4320) {
            return "calm";
        }
        return "sleepy";
    }

    private List<String> cleanInterests(List<String> raw) {
        List<String> result = new ArrayList<>();
        if (raw == null) {
            return result;
        }
        Set<String> seen = new HashSet<>();
        for (String s : raw) {
            if (s == null) continue;
            String t = s.trim();
            if (t.isEmpty()) continue;
            if (t.length() > 32) {
                throw new BizException("每个兴趣最多 32 个字");
            }
            if (seen.add(t)) result.add(t);
        }
        if (result.size() > 5) {
            throw new BizException("最多添加 5 个兴趣");
        }
        return result;
    }
}