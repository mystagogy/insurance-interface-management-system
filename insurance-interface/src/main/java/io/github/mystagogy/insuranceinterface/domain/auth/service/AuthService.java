package io.github.mystagogy.insuranceinterface.domain.auth.service;

import io.github.mystagogy.insuranceinterface.domain.auth.dto.LoginRequest;
import io.github.mystagogy.insuranceinterface.domain.auth.dto.LoginResponse;
import io.github.mystagogy.insuranceinterface.domain.auth.dto.MyInfoResponse;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    public LoginResponse login(LoginRequest request) {
        String token = "mock-token-for-" + request.username();
        return new LoginResponse(token, "Bearer");
    }

    public MyInfoResponse myInfo() {
        return new MyInfoResponse("demo-user", "ROLE_ADMIN");
    }
}

