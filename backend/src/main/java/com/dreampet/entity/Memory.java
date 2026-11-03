package com.dreampet.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("memories")
public class Memory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long petId;
    /** interest / event / profile / task / milestone */
    private String memoryType;
    private String content;
    /** 1~5，越大越重要，组装上下文时优先取高重要度 */
    private Integer importance;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
