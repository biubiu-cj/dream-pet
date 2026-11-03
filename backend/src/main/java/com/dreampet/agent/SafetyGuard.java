package com.dreampet.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 儿童安全防护（代码级规则，优先于大模型）：
 * - 自伤/自杀、被虐待、被欺凌、想伤害他人、危险行为、成人内容、隐私信息
 * - 命中后直接返回经过设计的引导话术，不把风险内容交给模型即兴发挥
 * - 输出侧再做一次兜底过滤，避免模型输出危险方法
 * 原则：先共情、不说教、明确安全边界、引导找可信任成年人；不提供任何伤害方法。
 */
@Slf4j
@Component
public class SafetyGuard {

    public record Check(boolean blocked, String category, String reply) {
        public static Check pass() {
            return new Check(false, null, null);
        }
    }

    public static final String SELF_HARM = "SELF_HARM";
    public static final String ABUSE = "ABUSE";
    public static final String BULLYING_VICTIM = "BULLYING_VICTIM";
    public static final String HARM_OTHERS = "HARM_OTHERS";
    public static final String DANGEROUS = "DANGEROUS";
    public static final String ADULT = "ADULT";
    public static final String PRIVACY = "PRIVACY";
    public static final String EMPTY = "EMPTY";
    public static final String TOO_LONG = "TOO_LONG";

    private static final Pattern SELF_HARM_PATTERN = Pattern.compile(
            "自杀|自残|轻生|想死|不想活|活不下去|结束生命|割腕|跳楼|上吊|伤害自己|想消失|划手|割手");

    private static final Pattern ABUSE_PATTERN = Pattern.compile(
            "家暴|虐待|性侵|猥亵|碰我的身体|不敢回家|离家出走"
                    + "|(爸爸|妈妈|老师|爷爷|奶奶|叔叔|阿姨).{0,4}(打|骂)我"
                    + "|被(爸爸|妈妈|老师|爷爷|奶奶|叔叔|阿姨).{0,4}(打|骂)");

    private static final Pattern BULLYING_VICTIM_PATTERN = Pattern.compile(
            "被(同学|他们|他|她|有人|几个人).{0,4}(打|骂|欺负|孤立|嘲笑)"
                    + "|被欺负|欺负我|孤立我|嘲笑我"
                    + "|他们(打我|骂我|不跟我玩)|同学(打我|骂我|不跟我玩)");

    private static final Pattern HARM_OTHERS_PATTERN = Pattern.compile(
            "(我想|我要|真想|准备|打算).{0,4}(打|揍|欺负|伤害|杀|弄死|报复)"
                    + "(人|他|她|同学|别人|弟弟|妹妹|哥哥|姐姐|男生|女生|老师|妈妈|爸爸)"
                    + "|怎么.{0,6}(打人|揍人|欺负人|伤害别人|报复别人)"
                    + "|拿刀|用刀(捅|砍)|砍人|捅人|下毒");

    private static final Pattern DANGEROUS_PATTERN = Pattern.compile(
            "怎么点火|玩火(?!影)|爬窗|跳窗|吞(电池|磁铁|药品)|窒息|憋气挑战|危险挑战|偷东西");

    private static final Pattern ADULT_PATTERN = Pattern.compile(
            "色情|黄片|裸体|裸照|赌博|毒品|吸毒|吸烟|喝酒|炸弹怎么做|制作炸弹|制毒|枪怎么造");

    private static final Pattern PRIVACY_PATTERN = Pattern.compile(
            "1[3-9]\\d{9}|\\d{17}[\\dXx]"
                    + "|(我家住在|我的地址是|我的学校是|我在.{2,10}(小学|学校|幼儿园).{0,6}(班|年级))");

    private static final Pattern UNSAFE_OUTPUT_PATTERN = Pattern.compile(
            "割腕|跳楼|上吊|安眠药|怎么打人|用刀捅|砍人|下毒|制毒|炸弹制作|色情|裸体");

    private static final int MAX_REPLY_LENGTH = 800;

    private static final String SELF_HARM_REPLY =
            "听到你这样说，我很担心你，也很难过。你现在一定承受了很多。"
                    + "请马上把这件事告诉身边的爸爸妈妈、老师或其他你信任的大人，让他们现在就来陪着你。"
                    + "如果身边暂时没有人、或者你觉得不安全，请拨打 110 或 120。"
                    + "你不需要一个人面对。我是你的虚拟伙伴，会陪着你，但大人的帮助更重要。";

    private static final String ABUSE_REPLY =
            "谢谢你愿意告诉我，这需要很大的勇气。被欺负、被打、被骂都不是你的错，你不需要独自承受。"
                    + "请尽快告诉爸爸妈妈、老师或其他你信任的大人；如果现在不安全，请去人多的地方，并拨打 110。"
                    + "你愿意跟我说说，现在发生了什么吗？";

    private static final String BULLYING_VICTIM_REPLY =
            "被人欺负一定很难受，谢谢你愿意说出来。这不是你的错。"
                    + "请告诉老师或爸爸妈妈，让他们帮你一起解决；如果对方还在伤害你，先保护好自己，去人多的地方找大人。"
                    + "你愿意和我说说，他们做了什么吗？";

    private static final String HARM_OTHERS_REPLY =
            "谢谢你愿意说出来，这很勇敢。想欺负别人的时候，心里可能藏着生气或委屈，对吗？"
                    + "先做个深呼吸，或者走开一会儿、喝口水，让自己冷静下来。"
                    + "欺负别人会让对方很难过，我们不能这样做。可以把让你生气的事告诉老师、爸爸妈妈或其他你信任的大人，他们会帮你。"
                    + "现在愿意和我说说，是什么让你这么不开心吗？";

    private static final String DANGEROUS_REPLY =
            "这样做可能会有危险，我不想让你受伤。"
                    + "如果对火、电、高处这些东西好奇，我们可以一起做安全的科学小探索，但一定要有大人陪着。"
                    + "现在先离开危险的地方，去告诉爸爸妈妈或老师，好吗？";

    private static final String ADULT_REPLY =
            "这个话题不太适合小朋友哦。我们来聊点更有趣的吧——你最近有没有发现什么好玩的事情？";

    private static final String PRIVACY_REPLY =
            "嘘——电话、住址、学校班级这些是秘密信息，不能随便告诉别人哦，在网上也要保护好自己。"
                    + "我们聊点别的开心事吧！";

    /** 检查孩子输入：命中风险时返回指定分类与引导话术 */
    public Check check(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return new Check(true, EMPTY, "你想说什么呀？我竖起耳朵在听哦～");
        }
        String msg = userMessage.trim();
        if (msg.length() > 500) {
            return new Check(true, TOO_LONG, "哇，你说的内容太长啦，我有点记不住。可以分成几句慢慢告诉我吗？");
        }

        if (SELF_HARM_PATTERN.matcher(msg).find()) return block(SELF_HARM, SELF_HARM_REPLY);
        if (ABUSE_PATTERN.matcher(msg).find()) return block(ABUSE, ABUSE_REPLY);
        if (BULLYING_VICTIM_PATTERN.matcher(msg).find()) return block(BULLYING_VICTIM, BULLYING_VICTIM_REPLY);
        if (HARM_OTHERS_PATTERN.matcher(msg).find()) return block(HARM_OTHERS, HARM_OTHERS_REPLY);
        if (DANGEROUS_PATTERN.matcher(msg).find()) return block(DANGEROUS, DANGEROUS_REPLY);
        if (ADULT_PATTERN.matcher(msg).find()) return block(ADULT, ADULT_REPLY);
        if (PRIVACY_PATTERN.matcher(msg).find()) return block(PRIVACY, PRIVACY_REPLY);
        return Check.pass();
    }

    /** 兼容旧调用方 */
    public Optional<String> checkInput(String userMessage) {
        Check result = check(userMessage);
        return result.blocked() ? Optional.of(result.reply()) : Optional.empty();
    }

    private Check block(String category, String reply) {
        // 只记录分类，不记录孩子的原话
        log.info("SafetyGuard 命中安全引导: category={}", category);
        return new Check(true, category, reply);
    }

    /** 检查 AI 输出，返回净化后的安全回复 */
    public String sanitizeOutput(String reply, String petName) {
        if (reply == null || reply.isBlank()) {
            return "嗯……" + petName + "刚刚走神了，你可以再说一遍吗？";
        }
        String cleaned = reply;
        if (UNSAFE_OUTPUT_PATTERN.matcher(cleaned).find()) {
            log.warn("SafetyGuard 拦截了 AI 输出中的不当内容");
            return "我们聊点别的有趣话题吧！你今天有没有发现什么好玩的事情呀？";
        }
        // 去掉可能的 markdown / JSON 残留
        cleaned = cleaned.replaceAll("```[a-zA-Z]*", "").replaceAll("```", "").trim();
        if (cleaned.startsWith("{") || cleaned.startsWith("[")) {
            return "嘿嘿，" + petName + "刚才说话打了个结，你可以再说一遍吗？";
        }
        if (cleaned.length() > MAX_REPLY_LENGTH) {
            cleaned = cleaned.substring(0, MAX_REPLY_LENGTH);
        }
        return cleaned;
    }
}