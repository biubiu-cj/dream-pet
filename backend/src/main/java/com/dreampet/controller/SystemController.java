package com.dreampet.controller;

import com.dreampet.common.ApiResponse;
import com.dreampet.dto.SystemDtos;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    @GetMapping("/info")
    public ApiResponse<SystemDtos.SystemInfo> info() {
        return ApiResponse.ok(new SystemDtos.SystemInfo("real", "DreamPet"));
    }
}