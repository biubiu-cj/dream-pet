package com.dreampet.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_messages")
public class ChatMessage {

    /** 普通对话消息 */
    public static final String TYPE_NORMAL = "normal";
    /** 「欢迎回来」记忆问候（用于结构化去重，不依赖文本前缀） */
    public static final String TYPE_GREETING = "greeting";
    /** 新对话分隔标记（系统消息，不展示；用于只读取当前对话上下文） */
    public static final String TYPE_SESSION_START = "session_start";

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long petId;
    /** user / assistant / system */
    private String role;
    private String content;
    /** normal / greeting；存量数据默认 normal */
    private String messageType;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
