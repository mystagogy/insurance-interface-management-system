package io.github.mystagogy.insuranceinterface.domain.stat.dto;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record IndemnityInsuranceStatQueryRequest(
    @Parameter(description = "조회 시작년월", example = "202401")
    @NotBlank(message = "조회 시작년월은 필수입니다.")
    @Pattern(regexp = "\\d{6}", message = "조회 시작년월은 yyyyMM 형식이어야 합니다.")
    String fromYm,

    @Parameter(description = "조회 종료년월", example = "202404")
    @NotBlank(message = "조회 종료년월은 필수입니다.")
    @Pattern(regexp = "\\d{6}", message = "조회 종료년월은 yyyyMM 형식이어야 합니다.")
    String toYm
) {
}
