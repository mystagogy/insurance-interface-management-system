package io.github.mystagogy.insuranceinterface.domain.history.dto;

public record HistoryItemResponse(
    String requestId,
    String requestTime,
    String interfaceName,
    String status,
    String errorMessage
) {
}
