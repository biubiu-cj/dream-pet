package com.dreampet.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pets")
public class Pet {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String name;
    /** fox / rabbit / cat / bear / dino */
    private String species;
    /** curious / gentle / brave / clever / calm */
    private String personality;
    private Integer level;
    private Integer experience;
    /** seed / sprout / bloom / dream */
    private String growthStage;
    private String avatarKey;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
