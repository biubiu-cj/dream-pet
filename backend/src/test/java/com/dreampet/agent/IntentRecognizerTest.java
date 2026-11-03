package com.dreampet.agent;

import com.dreampet.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 意图识别规则单测（不访问模型） */
class IntentRecognizerTest {

    private IntentRecognizer recognizer;

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties();
        props.getChat().setIntentLlmEnabled(false);
        recognizer = new IntentRecognizer(null, props);
    }

    @Test
    void greetingAndFarewell() {
        assertEquals(Intent.GREETING, recognizer.recognize("你好呀"));
        assertEquals(Intent.GREETING, recognizer.recognize("嗨，星星"));
        assertEquals(Intent.FAREWELL, recognizer.recognize("晚安，明天见"));
    }

    @Test
    void interestAndDream() {
        assertEquals(Intent.INTEREST_SHARE, recognizer.recognize("我最喜欢恐龙了"));
        assertEquals(Intent.INTEREST_SHARE, recognizer.recognize("我对宇宙很感兴趣"));
        assertEquals(Intent.DREAM_SHARE, recognizer.recognize("我以后想成为科学家"));
        assertEquals(Intent.DREAM_SHARE, recognizer.recognize("我的梦想是当一名画家"));
    }

    @Test
    void taskIntents() {
        assertEquals(Intent.TASK_REQUEST, recognizer.recognize("我想做个小任务"));
        assertEquals(Intent.TASK_REQUEST, recognizer.recognize("好无聊呀，有什么好玩的吗"));
        assertEquals(Intent.TASK_PROGRESS, recognizer.recognize("我完成了恐龙探索任务"));
    }

    @Test
    void memoryAndEmotion() {
        assertEquals(Intent.MEMORY_RECALL, recognizer.recognize("你还记得上次我们做了什么吗"));
        assertEquals(Intent.MEMORY_RECALL, recognizer.recognize("我们继续之前的冒险吧"));
        assertEquals(Intent.EMOTION_NEGATIVE, recognizer.recognize("我今天有点难过"));
        assertEquals(Intent.EMOTION_POSITIVE, recognizer.recognize("今天太开心了"));
    }

    @Test
    void questionAndSmallTalk() {
        assertEquals(Intent.QUESTION, recognizer.recognize("为什么星星会发光？"));
        assertEquals(Intent.QUESTION, recognizer.recognize("恐龙是怎么灭绝的"));
        assertEquals(Intent.SMALL_TALK, recognizer.recognize("今天中午吃了面条"));
    }
}