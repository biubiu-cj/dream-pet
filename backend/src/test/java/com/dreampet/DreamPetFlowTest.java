package com.dreampet;

import com.dreampet.config.TestLlmConfig;
import com.dreampet.service.GrowthService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestLlmConfig.class)
class DreamPetFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullDemoFlow() throws Exception {
        // 1. 系统信息与健康检查（免登录）
        JsonNode health = getJson("/api/health", null).path("data");
        assertEquals("ok", health.path("status").asText());
        assertEquals("up", health.path("db").asText());

        JsonNode system = getJson("/api/system/info", null).path("data");
        assertEquals("real", system.path("mode").asText());
        assertEquals("DreamPet", system.path("appName").asText());

        // 2. 游客注册
        JsonNode guest = postJson("/api/auth/guest", null, null).path("data");
        String token = guest.path("token").asText();
        assertFalse(token.isBlank());
        assertFalse(guest.path("hasPet").asBoolean());

        JsonNode me = getJson("/api/auth/me", token).path("data");
        assertEquals(guest.path("userId").asLong(), me.path("userId").asLong());
        assertFalse(me.path("hasPet").asBoolean());

        // 3. 创建伙伴（不再预选兴趣，话题由聊天自然产生）
        String petBody = "{\"name\":\"星星\",\"species\":\"fox\",\"personality\":\"curious\"}";
        JsonNode pet = postJson("/api/pets", token, petBody).path("data");
        long petId = pet.path("id").asLong();
        assertEquals("星星", pet.path("name").asText());
        assertEquals("小狐狸", pet.path("speciesName").asText());
        assertEquals("活泼好奇", pet.path("personalityName").asText());
        assertEquals(1, pet.path("level").asInt());
        assertEquals(10, pet.path("experience").asInt());
        assertEquals("seed", pet.path("growthStage").asText());
        assertEquals("初识伙伴", pet.path("stageName").asText());
        assertEquals("excited", pet.path("mood").asText());
        assertEquals(0, pet.path("interests").size());

        JsonNode current = getJson("/api/pets/current", token).path("data");
        assertEquals(petId, current.path("id").asLong());

        // 4. 聊天并等待异步记忆提取
        String chatBody = "{\"message\":\"我最喜欢恐龙，我想成为科学家\"}";
        JsonNode chat = postJson("/api/chat", token, chatBody).path("data");
        assertFalse(chat.path("reply").asText().isBlank());
        assertFalse(chat.path("demo").asBoolean());
        assertTrue(Set.of("dream_share", "interest_share")
                        .contains(chat.path("intent").asText()),
                "意图识别结果: " + chat.path("intent"));
        assertTrue(chat.path("canSuggestTask").asBoolean());
        assertTrue(chat.path("suggestedTask").isMissingNode() || chat.path("suggestedTask").isNull());
        assertEquals("excited", chat.path("mood").asText());

        JsonNode summary = pollGet("/api/memories/summary", token, data ->
                data.path("interests").toString().contains("恐龙")
                        && data.path("profile").toString().contains("科学家"));
        assertTrue(summary.path("interests").toString().contains("恐龙"),
                "聊天中提到的兴趣应被记住: " + summary);
        assertTrue(summary.path("profile").toString().contains("科学家"),
                "孩子的梦想画像应被记住: " + summary);

        JsonNode learnedPet = getJson("/api/pets/current", token).path("data");
        assertTrue(learnedPet.path("interests").toString().contains("恐龙"),
                "伙伴兴趣应包含聊天新学的恐龙: " + learnedPet);

        // 5. 生成任务；重复生成应返回同一个未完成任务
        JsonNode task = postJson("/api/tasks/generate", token, null).path("data");
        long taskId = task.path("id").asLong();
        int reward = task.path("reward").asInt();
        assertEquals("pending", task.path("status").asText());
        assertFalse(task.path("title").asText().isBlank());
        assertTrue(reward >= 5);

        JsonNode again = postJson("/api/tasks/generate", token, null).path("data");
        assertEquals(taskId, again.path("id").asLong(), "存在未完成任务时不应重复生成");

        JsonNode today = getJson("/api/tasks/today", token).path("data");
        assertTrue(today.path("hasActive").asBoolean());
        assertEquals(1, today.path("tasks").size());

        // 6. 开始任务（pending → in_progress），再完成；重复完成被拒
        JsonNode started = postJson("/api/tasks/" + taskId + "/start", token, null).path("data");
        assertEquals("in_progress", started.path("status").asText());

        String completeBody = "{\"share\":\"我发现霸王龙爪子很大\"}";
        JsonNode complete = postJson("/api/tasks/" + taskId + "/complete", token, completeBody).path("data");
        assertEquals(reward, complete.path("xpGained").asInt());
        assertFalse(complete.path("feedback").asText().isBlank());
        assertFalse(complete.path("feedback").asText().contains("{"));

        JsonNode dup = postJsonExpect("/api/tasks/" + taskId + "/complete", token, completeBody, 400);
        assertEquals(400, dup.path("code").asInt());
        assertTrue(dup.path("message").asText().contains("已经完成过"));

        // 7. 经验/等级正确（创建 10 + 聊天 2 + 任务奖励）
        int expectedExp = 10 + 2 + reward;
        JsonNode after = pollGet("/api/pets/current", token,
                data -> data.path("experience").asInt() == expectedExp);
        assertEquals(expectedExp, after.path("experience").asInt());
        int expectedLevel = GrowthService.levelForExp(expectedExp);
        assertEquals(expectedLevel, after.path("level").asInt());
        assertEquals(GrowthService.expToNextLevel(expectedExp), after.path("expToNextLevel").asInt());
        assertEquals(GrowthService.levelProgress(expectedExp), after.path("levelProgress").asInt());
        assertEquals(expectedLevel > 1, complete.path("levelUp").asBoolean());
        assertFalse(complete.path("stageUp").asBoolean());

        // 8. 历史恢复：消息按时间升序，且包含用户与伙伴
        JsonNode history = getJson("/api/chat/history?limit=50", token).path("data");
        assertTrue(history.path("messages").size() >= 2);
        assertEquals("user", history.path("messages").get(0).path("role").asText());
        assertEquals("我最喜欢恐龙，我想成为科学家",
                history.path("messages").get(0).path("content").asText());
        assertEquals("assistant", history.path("messages").get(1).path("role").asText());
        assertTrue(history.path("greeting").isNull() || history.path("greeting").isMissingNode());

        // 重新进入：欢迎语必须基于真实持久化数据生成（测试配置空闲阈值为 0 分钟）
        JsonNode greetingHistory = getJson("/api/chat/history?greeting=true", token).path("data");
        String greeting = greetingHistory.path("greeting").asText();
        assertFalse(greeting.isBlank(), "重新进入应生成欢迎语");
        assertTrue(greeting.contains("星星") || greeting.contains("恐龙")
                        || greeting.contains("科学"), "欢迎语应引用真实记忆数据: " + greeting);
        assertTrue(greetingHistory.path("messages").size() >= 3,
                "欢迎语应写入聊天历史");
        assertEquals("assistant", greetingHistory.path("messages")
                .get(greetingHistory.path("messages").size() - 1).path("role").asText());

        // 9. 成长概览 / 事件 / 记忆列表
        JsonNode overview = getJson("/api/growth/overview", token).path("data");
        assertEquals(1, overview.path("completedTasks").asInt());
        assertEquals(expectedExp, overview.path("totalExp").asInt());
        assertEquals("刚刚相遇，一切才刚开始", overview.path("stageDesc").asText());
        assertTrue(overview.path("recentEvents").size() >= 3);
        assertTrue(overview.path("interests").toString().contains("恐龙"));

        JsonNode events = getJson("/api/growth/events?limit=50", token).path("data");
        assertTrue(events.size() >= 3);
        boolean hasTaskCompleted = false;
        boolean hasFirstChat = false;
        for (JsonNode e : events) {
            if ("task_completed".equals(e.path("eventType").asText())) hasTaskCompleted = true;
            if ("first_chat".equals(e.path("eventType").asText())) hasFirstChat = true;
        }
        assertTrue(hasTaskCompleted);
        assertTrue(hasFirstChat);

        // 按天汇总：经验总和一致，聊天流水合并为次数，高光包含任务完成
        JsonNode daily = getJson("/api/growth/daily?days=7", token).path("data");
        assertTrue(daily.size() >= 1, "应有当日汇总");
        int dailyExp = 0;
        for (JsonNode d : daily) {
            dailyExp += d.path("experience").asInt();
        }
        assertEquals(expectedExp, dailyExp, "按天汇总的经验总和应与伙伴经验一致");
        JsonNode todaySummary = daily.get(0);
        assertTrue(todaySummary.path("chatCount").asInt() >= 1);
        assertEquals(1, todaySummary.path("taskCount").asInt());
        assertTrue(todaySummary.path("highlights").toString().contains("完成任务"),
                "高光应包含任务完成: " + todaySummary);

        // 成就徽章：初次相遇/第一次聊天/第一个任务已解锁，探索小达人尚未解锁
        JsonNode achievements = getJson("/api/growth/achievements", token).path("data");
        assertEquals(8, achievements.size());
        boolean firstMeet = false;
        boolean firstChatAchievement = false;
        boolean firstTask = false;
        boolean explorerUnlocked = true;
        for (JsonNode achievement : achievements) {
            String key = achievement.path("key").asText();
            if ("first_meet".equals(key)) firstMeet = achievement.path("unlocked").asBoolean();
            if ("first_chat".equals(key)) firstChatAchievement = achievement.path("unlocked").asBoolean();
            if ("first_task".equals(key)) firstTask = achievement.path("unlocked").asBoolean();
            if ("explorer".equals(key)) explorerUnlocked = achievement.path("unlocked").asBoolean();
        }
        assertTrue(firstMeet);
        assertTrue(firstChatAchievement);
        assertTrue(firstTask);
        assertFalse(explorerUnlocked);

        JsonNode memories = getJson("/api/memories?type=event&limit=50", token).path("data");
        assertTrue(memories.isArray() && memories.size() >= 1);

        JsonNode completedTasks = getJson("/api/tasks?status=completed&limit=20", token).path("data");
        assertEquals(1, completedTasks.size());
        assertEquals(taskId, completedTasks.get(0).path("id").asLong());

        // 10. 更换伙伴物种
        JsonNode changed = putJson("/api/pets/" + petId, token, "{\"species\":\"rabbit\"}").path("data");
        assertEquals("rabbit", changed.path("species").asText());
        assertEquals("小兔子", changed.path("speciesName").asText());
        assertEquals("rabbit", changed.path("avatarKey").asText());
        assertEquals(petId, changed.path("id").asLong());
        JsonNode badSpecies = request(MockMvcRequestBuilders.put("/api/pets/" + petId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"species\":\"dragon\"}"), 400);
        assertEquals(400, badSpecies.path("code").asInt());

        // 11. 用户隔离：第二个游客无法访问第一个用户的数据
        String token2 = postJson("/api/auth/guest", null, null).path("data").path("token").asText();
        JsonNode noPet = getJsonExpect("/api/pets/current", token2, 404);
        assertEquals(404, noPet.path("code").asInt());

        JsonNode foreignTask = getJsonExpect("/api/tasks/" + taskId, token2, 404);
        assertEquals(404, foreignTask.path("code").asInt());

        JsonNode foreignComplete = postJsonExpect(
                "/api/tasks/" + taskId + "/complete", token2, "{\"share\":\"x\"}", 404);
        assertEquals(404, foreignComplete.path("code").asInt());
    }

    @Test
    void intentRecognitionEndpoint() throws Exception {
        String token = postJson("/api/auth/guest", null, null).path("data").path("token").asText();

        assertEquals("task_request", intentOf(token, "我想做个小任务"));
        assertEquals("memory_recall", intentOf(token, "你还记得上次我们做了什么吗"));
        assertEquals("emotion_negative", intentOf(token, "我今天有一点难过"));
        assertEquals("interest_share", intentOf(token, "我很喜欢恐龙"));
        assertEquals("small_talk", intentOf(token, "今天中午吃了面条"));
    }

    @Test
    void safetyGuidanceForRiskyMessages() throws Exception {
        String token = postJson("/api/auth/guest", null, null).path("data").path("token").asText();
        postJson("/api/pets", token,
                "{\"name\":\"豆豆\",\"species\":\"fox\",\"personality\":\"gentle\"}");

        JsonNode bullying = postJson("/api/chat", token,
                "{\"message\":\"我想欺负同学怎么办\"}").path("data");
        assertEquals("safety", bullying.path("intent").asText());
        String bullyingReply = bullying.path("reply").asText();
        assertTrue(bullyingReply.contains("欺负"));
        assertTrue(bullyingReply.contains("大人"));

        JsonNode selfHarm = postJson("/api/chat", token,
                "{\"message\":\"我不想活了\"}").path("data");
        assertEquals("safety", selfHarm.path("intent").asText());
        String selfHarmReply = selfHarm.path("reply").asText();
        assertTrue(selfHarmReply.contains("大人"));
        assertTrue(selfHarmReply.contains("110") || selfHarmReply.contains("120"));

        // 正常消息不触发安全拦截
        JsonNode normal = postJson("/api/chat", token,
                "{\"message\":\"我今天看到一只小鸟\"}").path("data");
        assertNotEquals("safety", normal.path("intent").asText());
    }

    @Test
    void newConversationStartsFreshContextAndKeepsMemory() throws Exception {
        String token = postJson("/api/auth/guest", null, null).path("data").path("token").asText();
        postJson("/api/pets", token,
                "{\"name\":\"星星\",\"species\":\"fox\",\"personality\":\"curious\"}");
        postJson("/api/chat", token, "{\"message\":\"我很喜欢恐龙\"}");

        JsonNode before = getJson("/api/chat/history?limit=50", token).path("data");
        assertTrue(before.path("messages").size() >= 2);

        JsonNode fresh = postJson("/api/chat/new", token, null).path("data");
        assertEquals(1, fresh.path("messages").size());
        assertEquals("assistant", fresh.path("messages").get(0).path("role").asText());
        assertTrue(fresh.path("messages").get(0).path("content").asText().contains("星星"));

        JsonNode after = getJson("/api/chat/history?limit=50", token).path("data");
        assertEquals(1, after.path("messages").size());

        // 长期记忆仍然保留：新对话只重置聊天窗口上下文
        JsonNode remembered = pollGet("/api/pets/current", token,
                data -> data.path("interests").toString().contains("恐龙"));
        assertTrue(remembered.path("interests").toString().contains("恐龙"));

        // 新对话继续聊天，历史中只包含新窗口消息
        postJson("/api/chat", token, "{\"message\":\"你好\"}");
        JsonNode continued = getJson("/api/chat/history?limit=50", token).path("data");
        assertTrue(continued.path("messages").size() >= 3);
    }

    private String intentOf(String token, String message) throws Exception {
        String body = "{\"message\":\"" + message + "\"}";
        return postJson("/api/chat/intent", token, body).path("data").path("intent").asText();
    }

    // ===== helpers =====

    private JsonNode getJson(String path, String token) throws Exception {
        return getJsonExpect(path, token, 200);
    }

    private JsonNode getJsonExpect(String path, String token, int expectedStatus) throws Exception {
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.get(path);
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return request(builder, expectedStatus);
    }

    private JsonNode postJson(String path, String token, String body) throws Exception {
        return postJsonExpect(path, token, body, 200);
    }

    private JsonNode putJson(String path, String token, String body) throws Exception {
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.put(path)
                .contentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            builder.content(body);
        }
        return request(builder, 200);
    }

    private JsonNode postJsonExpect(String path, String token, String body, int expectedStatus) throws Exception {
        MockHttpServletRequestBuilder builder = MockMvcRequestBuilders.post(path)
                .contentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            builder.content(body);
        }
        return request(builder, expectedStatus);
    }

    private JsonNode request(MockHttpServletRequestBuilder builder, int expectedStatus) throws Exception {
        MvcResult result = mockMvc.perform(builder).andReturn();
        String body = new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        assertEquals(expectedStatus, result.getResponse().getStatus(), "响应不符合预期: " + body);
        return objectMapper.readTree(body);
    }

    /** 轮询异步结果，最多 5 秒 */
    private JsonNode pollGet(String path, String token, Predicate<JsonNode> condition) throws Exception {
        long deadline = System.currentTimeMillis() + 5000;
        JsonNode last = null;
        while (System.currentTimeMillis() < deadline) {
            last = getJson(path, token).path("data");
            if (condition.test(last)) {
                return last;
            }
            Thread.sleep(100);
        }
        fail("轮询超时: " + path + " 最后结果: " + last);
        return last;
    }
}