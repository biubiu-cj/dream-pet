package com.dreampet.llm;

import com.dreampet.agent.PromptTemplates;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mock LLM 客户端：不调用任何外部 API，基于规则 + 预置素材生成回复。
 * 通过解析 Prompt 中的 [TASK:XXX] 标记与「键: 值」上下文行，
 * 使用数据库中的真实数据（兴趣、任务、经历）生成回复，保证演示流程完整可信。
 */
public class MockLlmClient implements LlmClient {

    private final Random random = new Random();

    /** 各兴趣主题的趣味知识池（对话用） */
    private static final Map<String, String[]> INTEREST_FACTS = new LinkedHashMap<>();
    /** 各兴趣主题的任务池：{标题, 描述, 类别, 难度, 奖励} */
    private static final Map<String, Object[][]> TASK_POOL = new LinkedHashMap<>();

    static {
        INTEREST_FACTS.put("恐龙", new String[]{
                "你知道吗？霸王龙的前爪其实特别小，连自己的嘴巴都够不着呢！",
                "三角龙头上的三只角，其实是它保护自己的厉害武器！",
                "有些恐龙脖子超级长，能伸到很高很高的树上吃叶子，就像一台活体起重机！",
                "恐龙蛋其实没有想象中那么大，最大的恐龙蛋也就像篮球一样！"});
        INTEREST_FACTS.put("宇宙", new String[]{
                "你知道吗？月亮自己不会发光，它是反射太阳的光才亮晶晶的！",
                "火星是红色的，因为它的土壤里有很多铁锈一样的东西！",
                "太阳其实是一颗大大的恒星，比地球大一百多万倍呢！",
                "天上的星星很多都比太阳大，只是离得太远太远，看起来才那么小！"});
        INTEREST_FACTS.put("动物", new String[]{
                "你知道吗？小猫咪的胡须是它的『小尺子』，能帮它量洞口能不能钻过去！",
                "小兔子开心的时候会原地蹦起来转个圈，这叫『兔子舞』！",
                "小熊冬天睡觉的时候，心跳会变得特别特别慢，像按了慢放键！",
                "章鱼有三颗心脏，还特别聪明，会自己开瓶盖找吃的！"});
        INTEREST_FACTS.put("科学", new String[]{
                "你知道吗？彩虹其实是阳光穿过小水珠时变出的魔法，有七种颜色呢！",
                "冰、水、水蒸气其实是同一个东西变的，是不是很神奇？",
                "磁铁隔着一张纸也能吸住回形针，磁力会『穿墙术』哦！",
                "我们看到的太阳光，其实是太阳八分钟前发出的！"});
        INTEREST_FACTS.put("绘画", new String[]{
                "你知道吗？蓝色加黄色能变出绿色，红色加蓝色能变出紫色，颜色就像会变魔术！",
                "很多大画家小时候也喜欢乱涂乱画，画得开心最重要啦！",
                "画画不一定要像，把心里的感觉画出来就超棒！"});
        INTEREST_FACTS.put("故事", new String[]{
                "你知道吗？一个好故事通常有一个勇敢的主角、一个小麻烦，和一个温暖的结尾！",
                "睡前故事最早是很久很久以前，大人们围着篝火讲给小朋友听的！",
                "每一个故事的世界都是你自己创造的，你就是故事国王！"});
        INTEREST_FACTS.put("自然", new String[]{
                "你知道吗？一片树叶上住着一群小到看不见的小精灵，它们是大自然的小小清洁工！",
                "下雨后空气特别清新，因为雨水把灰尘都洗掉啦！",
                "蒲公英的种子像小伞兵，风一吹就飞去新的地方安家！"});
    }

    static {
        TASK_POOL.put("恐龙", new Object[][]{
                {"恐龙小侦探", "找出霸王龙和三角龙的三个不同之处，可以画下来或讲给家人听", "knowledge", 1, 10},
                {"恐龙餐厅", "想象一下：如果开一家恐龙餐厅，不同恐龙分别爱吃什么？设计一份恐龙菜单", "creative", 2, 15},
                {"恐龙脚印", "观察家里的小动物或玩具恐龙的脚印，猜猜它们走路的样子，学一种恐龙走路", "observe", 1, 10},
                {"穿越恐龙时代", "编一个你穿越到恐龙时代的小故事，讲讲你遇到了谁、发生了什么", "story", 2, 15}});
        TASK_POOL.put("宇宙", new Object[][]{
                {"宇宙探索家", "想象一下，如果你登上火星，会看到什么？把你的火星之旅画下来或讲出来", "creative", 2, 15},
                {"星星观察员", "晚上和家人一起看星星，找到最亮的一颗星，给它起个名字", "observe", 1, 10},
                {"星球设计师", "设计一颗属于你的星球：它是什么颜色？上面住着谁？有什么好玩的东西？", "creative", 2, 20},
                {"月亮变变变", "连续观察月亮几天的形状变化，说说你发现了什么规律", "observe", 2, 15}});
        TASK_POOL.put("动物", new Object[][]{
                {"动物观察员", "今天观察一种你喜欢的小动物，记录它的一个有趣特点", "observe", 1, 10},
                {"动物本领大比拼", "想一想：哪种动物跳得最高？哪种动物跑得最快？和家人比比谁知道得多", "knowledge", 1, 10},
                {"小小饲养员", "如果有机会照顾一种小动物，你想照顾谁？为它设计一天的生活安排", "creative", 2, 15}});
        TASK_POOL.put("科学", new Object[][]{
                {"厨房小科学家", "在家人陪同下，把盐或糖放进水里搅一搅，观察它们去哪儿了", "observe", 1, 10},
                {"神奇放大镜", "用放大镜观察三样家里的小东西，说说你发现了什么平时看不到的细节", "observe", 1, 10},
                {"为什么会这样", "选一个你觉得神奇的现象（比如冰会融化），猜猜原因，再和家人一起验证", "logic", 2, 15}});
        TASK_POOL.put("绘画", new Object[][]{
                {"梦想调色盘", "用三种你最喜欢的颜色，画一幅「我的心情」抽象画", "creative", 1, 10},
                {"给伙伴画像", "仔细观察你的虚拟伙伴，把它画在纸上，加上一个它最喜欢的东西", "creative", 1, 10},
                {"接龙画画", "和家人玩画画接龙：每人画一笔，最后看看变成了什么有趣的东西", "creative", 2, 15}});
        TASK_POOL.put("故事", new Object[][]{
                {"故事小作家", "用「有一天」「突然」「最后」三个词，编一个三句话的小故事", "story", 1, 10},
                {"如果我会飞", "编一个『如果我会飞』的小故事，讲讲你会飞去哪里、帮助谁", "story", 1, 10},
                {"故事改编家", "选一个你熟悉的故事，把结尾改成不一样的，讲给家人听", "story", 2, 15}});
        TASK_POOL.put("自然", new Object[][]{
                {"叶子收藏家", "出门找两片形状不一样的叶子，观察它们的纹路有什么不同", "observe", 1, 10},
                {"云朵想象家", "抬头看天上的云，找出三朵像某种东西的云，说说它们像什么", "creative", 1, 10},
                {"自然寻宝", "在小区或公园找到：一片落叶、一块小石头、一朵花（只看不摘），拍照或画下来", "observe", 2, 15}});
        Object[][] generic = {
                {"小小发明家", "想一想：生活中有什么事情可以变得更有趣？发明一个小办法并说出来", "creative", 2, 15},
                {"今日小帮手", "帮家人做一件小事（比如摆碗筷），然后说说做这件事的感觉", "life", 1, 5},
                {"问题大王", "想出三个你最想知道答案的问题，挑一个和家人一起找答案", "logic", 1, 10},
                {"快乐分享家", "回忆今天最开心的一件事，用三句话讲给家人或伙伴听", "life", 1, 5}};
        TASK_POOL.put("通用", generic);
    }

    private static final Pattern P_NAME = Pattern.compile("伙伴名字: (.+)");
    private static final Pattern P_INTERESTS = Pattern.compile("孩子兴趣: (.+)");
    private static final Pattern P_RECENT_TITLES = Pattern.compile("最近任务标题: (.+)");
    private static final Pattern P_STAGE = Pattern.compile("伙伴成长阶段: (.+)");
    private static final Pattern P_LAST_TASK = Pattern.compile("最近一次任务: (.+)");
    private static final Pattern P_DAYS = Pattern.compile("距上次聊天: (\\d+) 天");
    private static final Pattern P_TASK_TITLE = Pattern.compile("任务标题: (.+)");
    private static final Pattern P_SHARE = Pattern.compile("孩子的完成分享: (.+)");
    private static final Pattern P_ACTIVE_TASK = Pattern.compile("当前任务: (.+)");
    private static final Pattern P_EXISTING_INTERESTS = Pattern.compile("已有兴趣: (.*)");

    @Override
    public String chat(List<LlmMessage> messages) {
        String system = messages.stream().filter(m -> "system".equals(m.role()))
                .map(LlmMessage::content).findFirst().orElse("");
        String userMsg = messages.get(messages.size() - 1).content();

        if (system.startsWith(PromptTemplates.TAG_COMPANION)) return companion(system, userMsg);
        if (system.startsWith(PromptTemplates.TAG_TASK_GEN)) return taskGen(system);
        if (system.startsWith(PromptTemplates.TAG_MEMORY_EXTRACT)) return memoryExtract(system);
        if (system.startsWith(PromptTemplates.TAG_SUMMARY)) return summary(system);
        if (system.startsWith(PromptTemplates.TAG_FEEDBACK)) return feedback(system);
        if (system.startsWith(PromptTemplates.TAG_GREETING)) return greeting(system);
        return "哇，这个问题真有意思！让我想一想……你可以再跟我说说吗？";
    }

    // ========== COMPANION ==========
    private String companion(String system, String userMsg) {
        String name = find(P_NAME, system, "你的伙伴");
        String interests = find(P_INTERESTS, system, "");
        String activeTask = find(P_ACTIVE_TASK, system, "（无）");
        String msg = userMsg.trim();

        // 打招呼
        if (msg.matches("(?i).*(你好|您好|嗨|哈喽|hello|hi|早安|早上好|午安|下午好).{0,6}")) {
            return pick(
                    "你好呀！我是" + name + "，今天见到你真开心！你今天过得怎么样呀？",
                    "嗨！" + name + "一直在等你呢！今天有什么好玩的事情想跟我分享吗？",
                    "你好你好！" + name + "的耳朵都竖起来啦，快跟我说说今天的故事！");
        }
        if (msg.matches("(?i).*(晚安|再见|拜拜|我走了|去睡觉).{0,6}")) {
            return pick(
                    "晚安！" + name + "会在梦里继续我们的冒险！明天见啦～",
                    "再见！记得想我哦，" + name + "会一直在这里等你回来！",
                    "拜拜～今天和你聊天真开心，" + name + "给你盖好小被子啦！");
        }
        // 表达喜欢
        Matcher like = Pattern.compile("我(?:最|很|超|特别)?(?:喜欢|爱|喜爱)([^，。,\\.！!？\\?]{1,12})").matcher(msg);
        if (like.find()) {
            String thing = like.group(1).trim();
            return pick(
                    "哇，" + thing + "！我记住啦！" + name + "也觉得" + thing + "特别有意思。关于" + thing + "，你最好奇的是什么呢？",
                    "太棒了！原来你喜欢" + thing + "呀！" + name + "把它记在我们的小本本上了。我们来一起探索" + thing + "的有趣事情好不好？");
        }
        // 表达梦想
        Matcher dream = Pattern.compile("我想(?:成为|当)([^，。,\\.！!？\\?]{1,12})").matcher(msg);
        if (dream.find()) {
            return pick(
                    "哇！" + dream.group(1).trim() + "！这个梦想闪闪发光！" + name + "会一直陪着你，我们一起为梦想努力探索吧！",
                    "太酷啦！" + name + "把这个梦想好好收藏起来了！想实现它的话，我们可以从很多有趣的小探索开始哦！");
        }
        // 兴趣关键词
        for (String interest : INTEREST_FACTS.keySet()) {
            if (msg.contains(interest) || (interest.equals("恐龙") && msg.matches(".*[霸王龙三角龙翼龙].*"))) {
                String fact = pick(INTEREST_FACTS.get(interest));
                return fact + " 关于" + interest + "，你还想知道什么呢？";
            }
        }
        if (!interests.isBlank() && !interests.contains("（")) {
            String first = interests.split(",")[0].trim();
            if (INTEREST_FACTS.containsKey(first) && msg.length() > 6) {
                return pick(INTEREST_FACTS.get(first)) + " 我们一起继续探索" + first + "吧！";
            }
        }
        // 问任务
        if (msg.contains("任务") && !activeTask.equals("（无）")) {
            return "我们现在有一个有趣的小任务：「" + activeTask + "」要不要一起完成它？完成后我会变得更厉害哦！";
        }
        // 情绪安抚
        if (msg.matches(".*(难过|伤心|不开心|想哭|委屈|生气|害怕).{0,20}")) {
            return pick(
                    "抱抱你～" + name + "陪在你身边。愿意跟我说说发生了什么吗？如果是很严重的事情，也可以告诉爸爸妈妈或者你信任的大人哦。",
                    "听到你不开心，" + name + "的小尾巴都垂下来了。深呼吸一下，我一直在这里陪着你，想说什么都可以。");
        }
        // 默认：好奇追问
        return pick(
                "哇，听起来好有趣！" + name + "想再多听一点，你可以给我讲讲细节吗？",
                "咦？这个我还真没想过！你觉得这是为什么呢？我们一起猜猜看！",
                "嘿嘿，和你聊天真开心！" + name + "突然想到一个好问题：如果这件事发生在恐龙身上，会发生什么呢？",
                "收到收到！" + name + "把这件事记在心里啦！那接下来你想聊点什么，还是我们做个小任务？");
    }

    // ========== TASK_GEN ==========
    private String taskGen(String system) {
        String interests = find(P_INTERESTS, system, "");
        String recentRaw = find(P_RECENT_TITLES, system, "");
        Set<String> recent = Arrays.stream(recentRaw.split("\\|"))
                .map(String::trim)
                .collect(java.util.stream.Collectors.toSet());

        List<Object[]> candidates = new ArrayList<>();
        for (String interest : interests.split(",")) {
            String key = interest.trim();
            for (String poolKey : TASK_POOL.keySet()) {
                if (key.contains(poolKey) || poolKey.contains(key)) {
                    candidates.addAll(Arrays.asList(TASK_POOL.get(poolKey)));
                }
            }
        }
        candidates.addAll(Arrays.asList(TASK_POOL.get("通用")));
        // 过滤最近出过的任务
        List<Object[]> fresh = candidates.stream()
                .filter(t -> !recent.contains(t[0]))
                .toList();
        Object[] task = fresh.isEmpty()
                ? candidates.get(random.nextInt(candidates.size()))
                : fresh.get(random.nextInt(fresh.size()));

        return "{\"title\":\"" + task[0] + "\",\"description\":\"" + task[1]
                + "\",\"category\":\"" + task[2] + "\",\"difficulty\":" + task[3]
                + ",\"reward\":" + task[4] + "}";
    }

    // ========== MEMORY_EXTRACT ==========
    private String memoryExtract(String system) {
        String existing = find(P_EXISTING_INTERESTS, system, "");
        Set<String> existingSet = new HashSet<>();
        for (String s : existing.split(",")) existingSet.add(s.trim());

        String userLine = Arrays.stream(system.split("\n"))
                .filter(l -> l.startsWith("孩子: ")).map(l -> l.substring(4)).findFirst().orElse("");

        List<String> interests = new ArrayList<>();
        Matcher like = Pattern.compile("(?:我(?:最|很|超|特别)?(?:喜欢|爱)|我对.{0,2}感兴趣)([^，。,\\.！!？\\?]{1,8})").matcher(userLine);
        while (like.find() && interests.size() < 3) {
            String v = like.group(1).trim();
            if (!v.isEmpty() && !existingSet.contains(v)) interests.add(v);
        }
        // 主题关键词兜底
        for (String key : INTEREST_FACTS.keySet()) {
            if (userLine.contains(key) && !existingSet.contains(key) && interests.size() < 3
                    && interests.stream().noneMatch(key::contains)) {
                interests.add(key);
            }
        }

        List<String> events = new ArrayList<>();
        Matcher dream = Pattern.compile("我想(?:成为|当)([^，。,\\.！!？\\?]{1,15})").matcher(userLine);
        if (dream.find()) events.add("孩子说想成为" + dream.group(1).trim());
        Matcher today = Pattern.compile("我今天([^，。,\\.]{2,30})").matcher(userLine);
        if (today.find()) events.add("孩子分享：今天" + today.group(1).trim());
        Matcher see = Pattern.compile("我(?:在|看到了?|发现了)([^，。,\\.]{2,25})").matcher(userLine);
        if (events.isEmpty() && see.find()) events.add("孩子分享了：" + see.group(0).trim());

        String profile = "";
        if (dream.reset(userLine).find()) profile = "想成为" + dream.group(1).trim();

        StringBuilder sb = new StringBuilder("{\"interests\":[");
        for (int i = 0; i < interests.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(interests.get(i)).append('"');
        }
        sb.append("],\"events\":[");
        for (int i = 0; i < events.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"').append(events.get(i)).append('"');
        }
        sb.append("],\"profile\":\"").append(profile).append("\"}");
        return sb.toString();
    }

    // ========== SUMMARY ==========
    private String summary(String system) {
        String name = find(P_NAME, system, "伙伴");
        List<String> topics = new ArrayList<>();
        String[] lines = system.split("\n");
        boolean inDialogue = false;
        for (String l : lines) {
            if (l.startsWith("新增对话:")) { inDialogue = true; continue; }
            if (inDialogue && l.startsWith("孩子: ")) {
                String content = l.substring(4);
                if (content.length() > 50) content = content.substring(0, 50);
                topics.add(content);
            }
        }
        String existing = find(Pattern.compile("既有摘要: (.+)"), system, "");
        StringBuilder sb = new StringBuilder();
        if (!existing.isBlank() && !existing.contains("（无）")) sb.append(existing).append(" ");
        sb.append("孩子和").append(name).append("聊了：");
        if (topics.isEmpty()) {
            sb.append("一些日常小事。");
        } else {
            sb.append(String.join("；", topics.subList(Math.max(0, topics.size() - 5), topics.size()))).append("。");
        }
        String result = sb.toString();
        return result.length() > 150 ? result.substring(0, 150) : result;
    }

    // ========== FEEDBACK ==========
    private String feedback(String system) {
        String name = find(P_NAME, system, "你的伙伴");
        String title = find(P_TASK_TITLE, system, "这个小任务");
        String share = find(P_SHARE, system, "");
        String base = "太棒啦！你完成了「" + title + "」！" + name + "开心得转了三个圈！";
        if (!share.isBlank() && !share.contains("（孩子没有留言）")) {
            String s = share.length() > 30 ? share.substring(0, 30) : share;
            base += "你说的「" + s + "」我认真听啦，真的很了不起！";
        } else {
            base += "完成探索的你闪闪发光！";
        }
        base += "要不要把这次的发现讲给家人听一听呀？";
        return base;
    }

    // ========== GREETING ==========
    private String greeting(String system) {
        String name = find(P_NAME, system, "你的伙伴");
        String interests = find(P_INTERESTS, system, "");
        String lastTask = find(P_LAST_TASK, system, "（暂无）");
        String days = find(P_DAYS, system, "0");
        List<String> events = new ArrayList<>();
        String[] lines = system.split("\n");
        boolean inEvents = false;
        for (String l : lines) {
            if (l.startsWith("最近重要经历:")) { inEvents = true; continue; }
            if (inEvents) {
                if (l.startsWith("- ")) events.add(l.substring(2));
                else if (!l.startsWith("（")) inEvents = false;
            }
        }

        StringBuilder sb = new StringBuilder("欢迎回来！我是" + name + "！");
        if (!"0".equals(days)) sb.append("我们有 ").append(days).append(" 天没见啦，我好想你！");
        if (!lastTask.isBlank() && !lastTask.contains("暂无")) {
            sb.append("上次我们一起完成了「").append(lastTask).append("」，你表现得超级棒！");
        } else if (!events.isEmpty()) {
            sb.append("我还记得").append(events.get(0)).append("！");
        }
        if (!interests.isBlank() && !interests.contains("（")) {
            String first = interests.split(",")[0].trim();
            sb.append("今天还想继续我们的").append(first).append("冒险吗？");
        } else {
            sb.append("今天想一起做点什么有趣的事呢？");
        }
        return sb.toString();
    }

    // ========== utils ==========
    private String find(Pattern p, String text, String def) {
        Matcher m = p.matcher(text);
        return m.find() ? m.group(1).trim() : def;
    }

    private String pick(String... options) {
        return options[random.nextInt(options.length)];
    }
}
