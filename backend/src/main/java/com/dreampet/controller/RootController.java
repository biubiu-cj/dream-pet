package com.dreampet.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 根路径健康响应。
 * 云托管平台若默认探测 "/" 而不是业务健康检查路径，这里返回 200，避免误判服务不可用。
 */
@RestController
public class RootController {

    @GetMapping({"/", "/health"})
    public Map<String, Object> root() {
        return Map.of("app", "DreamPet", "status", "ok");
    }
}