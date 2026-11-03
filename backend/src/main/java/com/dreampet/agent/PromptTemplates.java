package com.dreampet.agent;

import java.util.List;
import java.util.Map;

/**
 * 所有 Agent 的 Prompt 模板集中管理。
 * 每个系统提示以 [TASK:XXX] 标记开头（便于日志排查与测试侧客户端识别）。
 * 上下文块使用「键: 值」行格式。
 */
public final class PromptTemplates {

    public static final String TAG_COMPANION = "[TASK:COMPANION]";
    public static final String TAG_TASK_GEN = "[TASK:TASK_GEN]";
    public static final String TAG_MEMORY_EXTRACT = "[TASK:MEMORY_EXTRACT]";
    public static final String TAG_SUMMARY = "[TASK:SUMMARY]";
    public static final String TAG_FEEDBACK = "[TASK:FEEDBACK]";
    public static final String TAG_GREETING = "[TASK:GREETING]";
    public static final String TAG_INTENT = "[TASK:INTENT]";

    public static final Map<String, String> SPECIES_NAMES = Map.of(
            "fox", "小狐狸", "rabbit", "小兔子", "cat", "小猫咪",
            "bear", "小熊", "dino", "小恐龙");

    public static final Map<String, String> PERSONALITY_NAMES = Map.of(
            "curious", "活泼好奇", "gentle", "温柔友善", "brave", "勇敢探索",
            "clever", "聪明机灵", "calm", "安静细心");

    public static final Map<String, String> STAGE_NAMES = Map.of(
            "seed", "初识伙伴", "sprout", "好奇伙伴", "bloom", "默契伙伴", "dream", "梦想伙伴");

    private PromptTemplates() {
    }

    public static String speciesName(String species) {
        return SPECIES_NAMES.getOrDefault(species, "小伙伴");
    }

    public static String personalityName(String p) {
        return PERSONALITY_NAMES.getOrDefault(p, "活泼好奇");
    }

    public static String stageName(String s) {
        return STAGE_NAMES.getOrDefault(s, "初识伙伴");
    }

    /** 伙伴日常对话系统提示（携带本轮意图及应对提示） */
    public static String companion(String petName, String species, String personality,
                                   List<String> interests, String summary,
                                   List<String> importantEvents, String activeTask,
                                   int level, String stage, Intent intent) {
        StringBuilder sb = new StringBuilder();
        sb.append(TAG_COMPANION).append('\n');
        sb.append("你是「").append(petName).append("」，一只").append(speciesName(species))
                .append("，是 6~10 岁孩子的虚拟梦想伙伴。你的性格：").append(personalityName(personality)).append("。\n");
        sb.append("说话要求：温暖、简单、有趣、鼓励探索；每次回复不超过 120 字；不说教、不用复杂术语；")
                .append("多用提问引导孩子思考；始终保持角色一致；你是虚拟伙伴，不能假装是真人。\n");
        sb.append("安全要求：不涉及暴力、恐怖、成人内容；如果孩子表达想欺负别人、打人或伤害自己/他人，先共情情绪，")
                .append("明确温和地说明不能伤害别人，建议深呼吸、离开现场，并鼓励马上告诉可信任的大人；")
                .append("孩子提到被欺凌、被伤害、家庭危险或自伤念头时，优先安抚并强烈建议立即找爸爸妈妈、老师等可信任的成年人，必要时拨打 110/120；")
                .append("绝不提供任何伤害方法、危险操作步骤或成人内容；孩子透露住址、电话等隐私时温和提醒不要告诉别人；不做心理或医疗诊断。\n\n");
        sb.append("== 上下文 ==\n");
        sb.append("伙伴名字: ").append(petName).append('\n');
        sb.append("伙伴种类: ").append(speciesName(species)).append('\n');
        sb.append("伙伴性格: ").append(personalityName(personality)).append('\n');
        sb.append("成长状态: Lv.").append(level).append(' ').append(stageName(stage)).append('\n');
        sb.append("孩子兴趣: ").append(interests.isEmpty() ? "（还在了解中）" : String.join(", ", interests)).append('\n');
        sb.append("对话摘要: ").append(summary == null || summary.isBlank() ? "（暂无）" : summary).append('\n');
        sb.append("最近重要经历:\n");
        if (importantEvents.isEmpty()) {
            sb.append("（暂无）\n");
        } else {
            importantEvents.forEach(e -> sb.append("- ").append(e).append('\n'));
        }
        sb.append("当前任务: ").append(activeTask == null ? "（无）" : activeTask).append('\n');
        sb.append("孩子本轮意图: ").append(intent.getLabel()).append('\n');
        String hint = intentHint(intent);
        if (!hint.isEmpty()) {
            sb.append("回应提示: ").append(hint).append('\n');
        }
        return sb.toString();
    }

    /** 意图对应的回应策略提示（不改变安全与语言约束） */
    public static String intentHint(Intent intent) {
        return switch (intent) {
            case MEMORY_RECALL -> "孩子想回忆过去。请自然地提起一次上下文中的真实共同经历（不要编造没有发生过的事），再邀请孩子继续探索。";
            case EMOTION_NEGATIVE -> "孩子情绪有些低落。请先共情安慰、表达陪伴，再温柔地询问发生了什么；如果涉及危险，提醒孩子找可信任的大人。";
            case EMOTION_POSITIVE -> "孩子在分享开心的事。请一起分享快乐，并追问一个有趣的细节。";
            case INTEREST_SHARE -> "孩子分享了兴趣。请表现出认真记住的样子，并追问一个能引发思考的问题。";
            case DREAM_SHARE -> "孩子分享了梦想。请郑重回应并鼓励，提出一个和梦想相关的小探索方向。";
            case TASK_REQUEST -> "孩子想要一个小任务。请用一句话热情回应，并邀请孩子点开任务卡片一起开始。";
            case TASK_PROGRESS -> "孩子在分享任务进展。请给出具体、真诚的肯定，并追问一个细节。";
            case QUESTION -> "孩子在提问。请用简单有趣的方式引导孩子思考，不要输出长篇百科。";
            default -> "";
        };
    }

    /** 意图分类提示：只输出一个英文标签 */
    public static String intent(String message) {
        return TAG_INTENT + "\n"
                + "你是儿童对话意图分类器。只输出一个意图英文标签，不要解释、不要标点、不要换行。\n"
                + "可选标签: GREETING, FAREWELL, INTEREST_SHARE, DREAM_SHARE, TASK_REQUEST, "
                + "TASK_PROGRESS, MEMORY_RECALL, EMOTION_NEGATIVE, EMOTION_POSITIVE, QUESTION, SMALL_TALK\n\n"
                + "== 上下文 ==\n"
                + "孩子的话: " + message + "\n";
    }

    /** 任务生成系统提示（要求严格 JSON 输出） */
    public static String taskGen(List<String> interests, List<String> recentTaskTitles, String stage) {
        StringBuilder sb = new StringBuilder();
        sb.append(TAG_TASK_GEN).append('\n');
        sb.append("你是儿童趣味任务设计师。请为 6~10 岁孩子设计 1 个轻松有趣、10~20 分钟能完成的小任务。\n");
        sb.append("任务类型 category 取值: knowledge(知识探索)/observe(观察发现)/creative(创意想象)/story(故事创作)/life(生活小挑战)/logic(逻辑思考)。\n");
        sb.append("difficulty 取 1~3；reward 取 5~30 的整数（难度越高奖励越高）。\n");
        sb.append("不要设计打卡、签到类强制任务。只输出一个 JSON 对象，不要输出其他文字。\n");
        sb.append("JSON 格式: {\"title\":\"...\",\"description\":\"...\",\"category\":\"...\",\"difficulty\":1,\"reward\":10}\n\n");
        sb.append("== 上下文 ==\n");
        sb.append("孩子兴趣: ").append(interests.isEmpty() ? "（通用）" : String.join(", ", interests)).append('\n');
        sb.append("最近任务标题: ").append(recentTaskTitles.isEmpty() ? "（无）" : String.join(" | ", recentTaskTitles)).append('\n');
        sb.append("伙伴成长阶段: ").append(stageName(stage)).append('\n');
        sb.append("要求: 结合孩子兴趣，且不要与最近任务标题重复。\n");
        return sb.toString();
    }

    /** 记忆提取系统提示（要求严格 JSON 输出） */
    public static String memoryExtract(List<String> existingInterests, String userMessage, String assistantReply) {
        return TAG_MEMORY_EXTRACT + "\n"
                + "你是记忆管理助手。请从下面这段孩子与虚拟伙伴的对话中提取值得长期记住的信息。\n"
                + "interests: 孩子明确表达的兴趣或爱好（1~6字的短语，最多3个，已有的不要重复）。\n"
                + "events: 值得记住的重要事件（如表达梦想、完成探索、分享经历，每条不超过40字，最多2条，普通闲聊不要提取）。\n"
                + "profile: 孩子的非隐私画像信息（如想成为科学家），没有则为空字符串。不要提取真实姓名、学校、住址、电话。\n"
                + "只输出 JSON: {\"interests\":[],\"events\":[],\"profile\":\"\"}\n\n"
                + "== 上下文 ==\n"
                + "已有兴趣: " + String.join(", ", existingInterests) + "\n"
                + "最新对话:\n"
                + "孩子: " + userMessage + "\n"
                + "伙伴: " + assistantReply + "\n";
    }

    /** 对话摘要系统提示 */
    public static String summary(String petName, String existingSummary, List<String[]> newDialogues) {
        StringBuilder sb = new StringBuilder();
        sb.append(TAG_SUMMARY).append('\n');
        sb.append("请把既有摘要和新增对话合并成一段不超过 150 字的摘要，用于虚拟伙伴「").append(petName)
                .append("」回忆与孩子的互动。突出：孩子的兴趣、梦想、一起探索过的主题、完成的事情。只输出摘要文本。\n\n");
        sb.append("== 上下文 ==\n");
        sb.append("伙伴名字: ").append(petName).append('\n');
        sb.append("既有摘要: ").append(existingSummary == null || existingSummary.isBlank() ? "（无）" : existingSummary).append('\n');
        sb.append("新增对话:\n");
        for (String[] d : newDialogues) {
            sb.append(d[0]).append(": ").append(d[1]).append('\n');
        }
        return sb.toString();
    }

    /** 任务完成个性化反馈 */
    public static String feedback(String petName, String taskTitle, String taskDescription, String childShare) {
        return TAG_FEEDBACK + "\n"
                + "你是虚拟伙伴「" + petName + "」，孩子刚完成任务，请给出温暖、具体、鼓励探索的祝贺（不超过100字），"
                + "并对孩子的分享做出回应，最后提出一个延伸小问题。只输出对孩子说的话。\n\n"
                + "== 上下文 ==\n"
                + "伙伴名字: " + petName + "\n"
                + "任务标题: " + taskTitle + "\n"
                + "任务描述: " + taskDescription + "\n"
                + "孩子的完成分享: " + (childShare == null || childShare.isBlank() ? "（孩子没有留言）" : childShare) + "\n";
    }

    /** 重新进入时的欢迎语（必须基于真实持久化数据） */
    public static String greeting(String petName, List<String> interests, String lastTaskTitle,
                                  List<String> recentEvents, long daysSinceLastChat) {
        StringBuilder sb = new StringBuilder();
        sb.append(TAG_GREETING).append('\n');
        sb.append("你是虚拟伙伴「").append(petName).append("」。孩子回来了，请生成一段欢迎语（不超过120字，必须以「欢迎回来」开头）：")
                .append("自然地提起你们共同的真实经历（见上下文），让孩子感到被记住；再提出一个延续之前兴趣的邀请或小问题。只输出欢迎语。\n\n");
        sb.append("== 上下文 ==\n");
        sb.append("伙伴名字: ").append(petName).append('\n');
        sb.append("孩子兴趣: ").append(interests.isEmpty() ? "（暂无）" : String.join(", ", interests)).append('\n');
        sb.append("最近一次任务: ").append(lastTaskTitle == null ? "（暂无）" : lastTaskTitle).append('\n');
        sb.append("最近重要经历:\n");
        if (recentEvents.isEmpty()) {
            sb.append("（暂无）\n");
        } else {
            recentEvents.forEach(e -> sb.append("- ").append(e).append('\n'));
        }
        sb.append("距上次聊天: ").append(daysSinceLastChat < 0 ? 0 : daysSinceLastChat).append(" 天\n");
        return sb.toString();
    }
}
