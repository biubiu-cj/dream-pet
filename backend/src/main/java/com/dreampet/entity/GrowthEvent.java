package com.dreampet.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("growth_events")
public class GrowthEvent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long petId;
    /** pet_created / first_chat / task_completed / exp_gain / level_up / stage_up */
    private String eventType;
    private String description;
    private Integer experienceGained;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
