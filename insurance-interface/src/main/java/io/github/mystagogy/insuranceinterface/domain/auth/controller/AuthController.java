package io.github.mystagogy.insuranceinterface.domain.auth.controller;

import io.github.mystagogy.insuranceinterface.common.response.ApiResponse;
import io.github.mystagogy.insuranceinterface.domain.auth.dto.LoginRequest;
import io.github.mystagogy.insuranceinterface.domain.auth.dto.LoginResponse;
import io.github.mystagogy.insuranceinterface.domain.auth.dto.MyInfoResponse;
import io.github.mystagogy.insuranceinterface.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<MyInfoResponse> myInfo() {
        return ApiResponse.ok(authService.myInfo());
    }
}

