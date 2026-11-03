package com.dreampet.controller;

import com.dreampet.common.ApiResponse;
import com.dreampet.common.UserContext;
import com.dreampet.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @DeleteMapping
    public ApiResponse<Void> delete() {
        accountService.deleteAccount(UserContext.get());
        return ApiResponse.ok();
    }
}