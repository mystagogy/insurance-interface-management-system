package io.github.mystagogy.insuranceinterface.domain.history.dto;

import jakarta.validation.constraints.Pattern;

public record HistoryQueryRequest(
    @Pattern(regexp = "\\d{8}", message = "조회 시작일자는 yyyyMMdd 형식이어야 합니다.")
    String from,

    @Pattern(regexp = "\\d{8}", message = "조회 종료일자는 yyyyMMdd 형식이어야 합니다.")
    String to
) {
}
