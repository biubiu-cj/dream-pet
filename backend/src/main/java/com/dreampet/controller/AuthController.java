package com.dreampet.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dreampet.common.ApiResponse;
import com.dreampet.common.UserContext;
import com.dreampet.dto.AuthDtos;
import com.dreampet.entity.Pet;
import com.dreampet.entity.User;
import com.dreampet.mapper.PetMapper;
import com.dreampet.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PetMapper petMapper;

    @PostMapping("/register")
    public ApiResponse<AuthDtos.AuthResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest req) {
        return ApiResponse.ok(toAuthResponse(authService.register(req.getUsername(), req.getPassword())));
    }

    @PostMapping("/login")
    public ApiResponse<AuthDtos.AuthResponse> login(@Valid @RequestBody AuthDtos.LoginRequest req) {
        return ApiResponse.ok(toAuthResponse(authService.login(req.getUsername(), req.getPassword())));
    }

    @PostMapping("/guest")
    public ApiResponse<AuthDtos.AuthResponse> guest() {
        return ApiResponse.ok(toAuthResponse(authService.guest()));
    }

    @GetMapping("/me")
    public ApiResponse<AuthDtos.MeResponse> me() {
        Long userId = UserContext.get();
        User user = authService.me(userId);
        AuthDtos.MeResponse resp = new AuthDtos.MeResponse();
        resp.setUserId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setHasPet(hasPet(userId));
        return ApiResponse.ok(resp);
    }

    private AuthDtos.AuthResponse toAuthResponse(AuthService.AuthResult result) {
        AuthDtos.AuthResponse resp = new AuthDtos.AuthResponse();
        resp.setToken(result.token());
        resp.setUsername(result.username());
        resp.setUserId(result.userId());
        resp.setHasPet(hasPet(result.userId()));
        return resp;
    }

    private boolean hasPet(Long userId) {
        return petMapper.selectCount(new LambdaQueryWrapper<Pet>().eq(Pet::getUserId, userId)) > 0;
    }
}