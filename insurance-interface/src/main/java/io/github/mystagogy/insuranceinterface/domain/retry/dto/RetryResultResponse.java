package io.github.mystagogy.insuranceinterface.domain.retry.dto;

public record RetryResultResponse(String requestId, boolean accepted) {
}

