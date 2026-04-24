package io.github.mystagogy.insuranceinterface.domain.auth.dto;

public record CsrfTokenResponse(String token, String headerName) {
}
