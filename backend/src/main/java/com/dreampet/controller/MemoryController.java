package com.dreampet.controller;

import com.dreampet.common.ApiResponse;
import com.dreampet.common.UserContext;
import com.dreampet.dto.MemoryDtos;
import com.dreampet.service.MemoryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/memories")
@RequiredArgsConstructor
public class MemoryController {

    private final MemoryQueryService memoryQueryService;

    @GetMapping
    public ApiResponse<List<MemoryDtos.MemoryDTO>> list(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.ok(memoryQueryService.list(UserContext.get(), type, limit));
    }

    @GetMapping("/summary")
    public ApiResponse<MemoryDtos.MemorySummary> summary() {
        return ApiResponse.ok(memoryQueryService.summary(UserContext.get()));
    }
}