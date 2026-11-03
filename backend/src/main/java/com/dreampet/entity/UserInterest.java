package com.dreampet.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_interests")
public class UserInterest {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String interest;
    /** create / chat / task */
    private String source;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
