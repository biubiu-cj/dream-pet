package com.dreampet.controller;

import com.dreampet.agent.Intent;
import com.dreampet.agent.IntentRecognizer;
import com.dreampet.common.ApiResponse;
import com.dreampet.common.UserContext;
import com.dreampet.config.AppProperties;
import com.dreampet.dto.ChatDtos;
import com.dreampet.service.ChatService;
import com.dreampet.service.RateLimitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final IntentRecognizer intentRecognizer;
    private final RateLimitService rateLimitService;
    private final AppProperties props;

    @PostMapping
    public ApiResponse<ChatDtos.ChatResponse> chat(@Valid @RequestBody ChatDtos.ChatRequest req) {
        return ApiResponse.ok(chatService.chat(UserContext.get(), req.getMessage()));
    }

    /** 意图识别调试/展示接口：返回对孩子消息的意图判断 */
    @PostMapping("/intent")
    public ApiResponse<ChatDtos.IntentResponse> intent(@Valid @RequestBody ChatDtos.ChatRequest req) {
        Long userId = UserContext.get();
        rateLimitService.check(userId, "intent", props.getRateLimit().getIntentPerHour());
        Intent intent = intentRecognizer.recognize(req.getMessage());
        ChatDtos.IntentResponse resp = new ChatDtos.IntentResponse();
        resp.setIntent(intent.code());
        resp.setLabel(intent.getLabel());
        return ApiResponse.ok(resp);
    }

    /** 开启新对话：清空当前聊天窗口上下文（长期记忆与历史记录保留） */
    @PostMapping("/new")
    public ApiResponse<ChatDtos.HistoryResponse> newConversation() {
        return ApiResponse.ok(chatService.startNewConversation(UserContext.get()));
    }

    @GetMapping("/history")
    public ApiResponse<ChatDtos.HistoryResponse> history(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "false") boolean greeting) {
        return ApiResponse.ok(chatService.history(UserContext.get(), limit, greeting));
    }
}