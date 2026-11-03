package com.dreampet.agent;

/** 儿童消息意图分类结果 */
public enum Intent {
    GREETING("打招呼"),
    FAREWELL("道别"),
    INTEREST_SHARE("分享兴趣"),
    DREAM_SHARE("分享梦想"),
    TASK_REQUEST("想要任务"),
    TASK_PROGRESS("分享任务进展"),
    MEMORY_RECALL("回忆过去"),
    EMOTION_NEGATIVE("情绪低落"),
    EMOTION_POSITIVE("开心分享"),
    QUESTION("提问"),
    SMALL_TALK("闲聊");

    private final String label;

    Intent(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 返回给小写的接口字段值 */
    public String code() {
        return name().toLowerCase();
    }

    /** 从 LLM 返回文本中宽松匹配意图标签 */
    public static Intent fromText(String text) {
        if (text == null) return null;
        String upper = text.toUpperCase();
        for (Intent intent : values()) {
            if (upper.contains(intent.name())) return intent;
        }
        return null;
    }
}