package io.github.mystagogy.insuranceinterface.domain.auth.dto;

public record LoginResponse(
    String username,
    String role,
    String sessionId
) {
}
