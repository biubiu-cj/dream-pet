package com.dreampet.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dreampet.entity.Task;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface TaskMapper extends BaseMapper<Task> {

    /**
     * 条件更新完成任务：仅当状态不是 completed 时才生效。
     * 返回 0 表示任务已被完成过（防重复领取经验）。
     */
    @Update("UPDATE tasks SET status = 'completed', completed_at = CURRENT_TIMESTAMP " +
            "WHERE id = #{id} AND user_id = #{userId} AND status <> 'completed'")
    int completeTaskAtomic(@Param("id") Long id, @Param("userId") Long userId);
}
