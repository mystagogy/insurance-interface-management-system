package io.github.mystagogy.insuranceinterface.domain.auth.controller;

import io.github.mystagogy.insuranceinterface.common.response.ApiResponse;
import io.github.mystagogy.insuranceinterface.domain.auth.dto.LoginRequest;
import io.github.mystagogy.insuranceinterface.domain.auth.dto.LoginResponse;
import io.github.mystagogy.insuranceinterface.domain.auth.dto.MyInfoResponse;
import io.github.mystagogy.insuranceinterface.domain.auth.service.AuthService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
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

    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        required = true,
        description = "로그인 요청 정보",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = LoginRequest.class),
            examples = @ExampleObject(
                name = "요청 예시",
                value = """
                    {
                      "username": "test",
                      "password": "testpw"
                    }
                    """
            )
        )
    )
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
        return ApiResponse.ok(authService.login(request, httpServletRequest));
    }

    @GetMapping("/me")
    public ApiResponse<MyInfoResponse> myInfo() {
        return ApiResponse.ok(authService.myInfo());
    }
}
