package io.github.mystagogy.insuranceinterface.domain.stat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CarInsuranceContractStatQueryRequest(
    @NotBlank(message = "조회 시작년월은 필수입니다.")
    @Pattern(regexp = "\\d{6}", message = "조회 시작년월은 yyyyMM 형식이어야 합니다.")
    String fromYm,

    @NotBlank(message = "조회 종료년월은 필수입니다.")
    @Pattern(regexp = "\\d{6}", message = "조회 종료년월은 yyyyMM 형식이어야 합니다.")
    String toYm
) {
}
