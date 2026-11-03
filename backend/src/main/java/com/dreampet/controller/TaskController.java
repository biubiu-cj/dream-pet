package com.dreampet.controller;

import com.dreampet.common.ApiResponse;
import com.dreampet.common.UserContext;
import com.dreampet.dto.TaskDtos;
import com.dreampet.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping("/today")
    public ApiResponse<TaskDtos.TodayTasksResponse> today() {
        return ApiResponse.ok(taskService.today(UserContext.get()));
    }

    @PostMapping("/generate")
    public ApiResponse<TaskDtos.TaskDTO> generate() {
        return ApiResponse.ok(taskService.generate(UserContext.get()));
    }

    @GetMapping
    public ApiResponse<List<TaskDtos.TaskDTO>> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.ok(taskService.list(UserContext.get(), status, limit));
    }

    @GetMapping("/{id}")
    public ApiResponse<TaskDtos.TaskDTO> getById(@PathVariable Long id) {
        return ApiResponse.ok(taskService.getById(UserContext.get(), id));
    }

    @PostMapping("/{id}/start")
    public ApiResponse<TaskDtos.TaskDTO> start(@PathVariable Long id) {
        return ApiResponse.ok(taskService.start(UserContext.get(), id));
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<TaskDtos.CompleteTaskResponse> complete(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) TaskDtos.CompleteTaskRequest req) {
        String share = req == null ? null : req.getShare();
        return ApiResponse.ok(taskService.complete(UserContext.get(), id, share));
    }
}