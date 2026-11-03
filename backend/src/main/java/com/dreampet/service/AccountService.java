package com.dreampet.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dreampet.entity.*;
import com.dreampet.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 账号删除：级联清理该用户全部数据（儿童隐私合规） */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserMapper userMapper;
    private final PetMapper petMapper;
    private final UserInterestMapper interestMapper;
    private final TaskMapper taskMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final MemoryMapper memoryMapper;
    private final GrowthEventMapper growthEventMapper;
    private final ConversationSummaryMapper summaryMapper;

    @Transactional
    public void deleteAccount(Long userId) {
        chatMessageMapper.delete(new LambdaQueryWrapper<ChatMessage>().eq(ChatMessage::getUserId, userId));
        memoryMapper.delete(new LambdaQueryWrapper<Memory>().eq(Memory::getUserId, userId));
        growthEventMapper.delete(new LambdaQueryWrapper<GrowthEvent>().eq(GrowthEvent::getUserId, userId));
        summaryMapper.delete(new LambdaQueryWrapper<ConversationSummary>()
                .eq(ConversationSummary::getUserId, userId));
        taskMapper.delete(new LambdaQueryWrapper<Task>().eq(Task::getUserId, userId));
        interestMapper.delete(new LambdaQueryWrapper<UserInterest>().eq(UserInterest::getUserId, userId));
        petMapper.delete(new LambdaQueryWrapper<Pet>().eq(Pet::getUserId, userId));
        userMapper.deleteById(userId);
    }
}