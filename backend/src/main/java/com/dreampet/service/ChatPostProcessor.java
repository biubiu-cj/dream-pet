package com.dreampet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dreampet.agent.MemoryManager;
import com.dreampet.config.AppProperties;
import com.dreampet.entity.GrowthEvent;
import com.dreampet.entity.Pet;
import com.dreampet.mapper.GrowthEventMapper;
import com.dreampet.mapper.PetMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 聊天异步后处理：记忆提取、摘要、聊天经验、首次聊天事件。
 * 独立 Bean 以保证 @Async 代理生效，任何失败只记录日志，不影响聊天响应。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatPostProcessor {

    private final MemoryManager memoryManager;
    private final GrowthService growthService;
    private final PetMapper petMapper;
    private final GrowthEventMapper growthEventMapper;
    private final AppProperties props;

    /** 只有包含个人信息/偏好/经历线索的消息才值得调用模型提取记忆，降低调用成本 */
    private static final java.util.regex.Pattern MEMORY_CANDIDATE = java.util.regex.Pattern.compile(
            "我(喜欢|最爱|爱|想|要|希望|发现|看到|观察|学会|画|写|养|今天|昨天)|梦想|最怕|害怕|难过|开心|骄傲|朋友|老师|妈妈|爸爸|家人|学校");

    @Async
    public void process(Long userId, Long petId, String petName, String userMessage, String assistantReply) {
        if (MEMORY_CANDIDATE.matcher(userMessage == null ? "" : userMessage).find()) {
            try {
                memoryManager.extractAndSave(userId, petId, userMessage, assistantReply);
            } catch (Exception e) {
                log.warn("异步记忆提取失败: {}", e.getMessage());
            }
        }
        try {
            memoryManager.maybeSummarize(userId, petId, petName);
        } catch (Exception e) {
            log.warn("异步对话摘要失败: {}", e.getMessage());
        }
        try {
            Long firstChat = growthEventMapper.selectCount(new LambdaQueryWrapper<GrowthEvent>()
                    .eq(GrowthEvent::getUserId, userId)
                    .eq(GrowthEvent::getPetId, petId)
                    .eq(GrowthEvent::getEventType, "first_chat"));
            if (firstChat == 0) {
                growthService.recordEvent(userId, petId, "first_chat", "和伙伴的第一次聊天", 0);
            }
        } catch (Exception e) {
            log.warn("首次聊天事件写入失败: {}", e.getMessage());
        }
        try {
            Pet pet = petMapper.selectById(petId);
            if (pet != null) {
                growthService.addChatExp(userId, pet,
                        props.getGrowth().getChatExp(), props.getGrowth().getChatExpDailyCap());
            }
        } catch (Exception e) {
            log.warn("异步聊天经验结算失败: {}", e.getMessage());
        }
    }
}