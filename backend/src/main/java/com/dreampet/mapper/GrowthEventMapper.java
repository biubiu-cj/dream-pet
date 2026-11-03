package com.dreampet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dreampet.entity.GrowthEvent;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface GrowthEventMapper extends BaseMapper<GrowthEvent> {

    /** 统计用户今日通过聊天获得的经验（用于每日上限控制） */
    @Select("SELECT COALESCE(SUM(experience_gained), 0) FROM growth_events " +
            "WHERE user_id = #{userId} AND event_type = 'chat_exp' AND created_at >= CURRENT_DATE")
    int sumTodayChatExp(@Param("userId") Long userId);

    /** 统计用户有成长事件的天数（用于「细水长流」成就） */
    @Select("SELECT COUNT(DISTINCT CAST(created_at AS DATE)) FROM growth_events WHERE user_id = #{userId}")
    int countActiveDays(@Param("userId") Long userId);
}