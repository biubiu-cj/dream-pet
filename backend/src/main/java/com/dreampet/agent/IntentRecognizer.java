package com.dreampet.agent;

import com.dreampet.config.AppProperties;
import com.dreampet.llm.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 用户意图识别：
 * 1. 规则优先（零成本、低延迟，覆盖儿童常见表达）
 * 2. 规则无法判断且启用 LLM 兜底时，调用一次很短的分类请求（带 10 分钟缓存）
 *
 * 识别结果用于：驱动任务推荐、回忆类回复、情绪安抚提示等。
 * 识别失败不阻塞聊天，直接降级为「闲聊」。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IntentRecognizer {

    private static final Pattern EMOTION_NEGATIVE = Pattern.compile(
            "难过|伤心|不开心|想哭|委屈|生气|害怕|孤单|焦虑|被欺负|欺负我|没人(玩|理我)|讨厌自己");
    private static final Pattern EMOTION_POSITIVE = Pattern.compile(
            "开心|高兴|快乐|太棒了|好喜欢|激动|兴奋|好幸福");
    private static final Pattern TASK_PROGRESS = Pattern.compile(
            "我(完成了|做完了|找到了|发现了|观察了|画了|写了|做出了|挖到了)");
    private static final Pattern TASK_REQUEST = Pattern.compile(
            "任务|小挑战|玩点什么|有什么好玩|好玩的|无聊|推荐.*(任务|活动)|给我.*任务|安排.*任务");
    private static final Pattern MEMORY_RECALL = Pattern.compile(
            "记得|上次|之前|以前|回顾|我们(做过|聊过|一起)|继续.*(冒险|探索)|还记得");
    private static final Pattern DREAM_SHARE = Pattern.compile(
            "想(成为|当)|我的梦想|梦想.*(是|成为|当)");
    private static final Pattern INTEREST_SHARE = Pattern.compile(
            "我(最|很|超|特别)?(喜欢|爱)|我对.{0,6}(感兴趣|好奇)|迷上了");
    private static final Pattern FAREWELL = Pattern.compile(
            "晚安|再见|拜拜|明天见|我走了|下次见");
    private static final Pattern GREETING = Pattern.compile(
            "^(你好|您好|嗨|哈喽|hi|hello|早上好|早安|中午好|下午好|晚上好|在吗)");
    private static final Pattern QUESTION = Pattern.compile(
            "[?？]|为什么|怎么|是什么|什么是|多少|哪一个|哪个|可不可以|会不会");

    private final LlmService llmService;
    private final AppProperties props;

    public Intent recognize(String message) {
        if (message == null || message.isBlank()) return Intent.SMALL_TALK;
        Intent rule = ruleBased(message);
        if (rule != Intent.SMALL_TALK) return rule;

        // 规则无法区分闲聊与更细腻的意图时，用真实模型做一次短分类（可配置关闭）
        if (!props.getChat().isIntentLlmEnabled() || message.trim().length() < 8) {
            return rule;
        }
        try {
            String raw = llmService.completeCached(PromptTemplates.intent(message));
            Intent llmIntent = Intent.fromText(raw);
            if (llmIntent == null) return rule;
            log.debug("意图识别(LLM): {}", llmIntent);
            return llmIntent;
        } catch (Exception e) {
            // 意图识别失败不影响主流程
            log.warn("意图识别降级为规则结果: {}", e.getMessage());
            return rule;
        }
    }

    /** 纯规则分类（供单元测试与无 LLM 场景使用） */
    public Intent ruleBased(String message) {
        String msg = message.trim();
        if (EMOTION_NEGATIVE.matcher(msg).find()) return Intent.EMOTION_NEGATIVE;
        if (TASK_PROGRESS.matcher(msg).find()) return Intent.TASK_PROGRESS;
        if (TASK_REQUEST.matcher(msg).find()) return Intent.TASK_REQUEST;
        if (MEMORY_RECALL.matcher(msg).find()) return Intent.MEMORY_RECALL;
        if (DREAM_SHARE.matcher(msg).find()) return Intent.DREAM_SHARE;
        if (INTEREST_SHARE.matcher(msg).find()) return Intent.INTEREST_SHARE;
        if (EMOTION_POSITIVE.matcher(msg).find()) return Intent.EMOTION_POSITIVE;
        if (FAREWELL.matcher(msg).find()) return Intent.FAREWELL;
        if (GREETING.matcher(msg).find()) return Intent.GREETING;
        if (QUESTION.matcher(msg).find()) return Intent.QUESTION;
        return Intent.SMALL_TALK;
    }

    public List<Intent> allIntents() {
        return List.of(Intent.values());
    }
}