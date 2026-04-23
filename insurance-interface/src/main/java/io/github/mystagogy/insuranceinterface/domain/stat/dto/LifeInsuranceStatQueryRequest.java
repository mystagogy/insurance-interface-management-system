package io.github.mystagogy.insuranceinterface.domain.stat.dto;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LifeInsuranceStatQueryRequest(
    @Parameter(description = "조회 시작년도", example = "2024")
    @NotBlank(message = "조회 시작년도는 필수입니다.")
    @Pattern(regexp = "\\d{4}", message = "조회 시작년도는 yyyy 형식이어야 합니다.")
    String fromYear,

    @Parameter(description = "조회 종료년도", example = "2024")
    @NotBlank(message = "조회 종료년도는 필수입니다.")
    @Pattern(regexp = "\\d{4}", message = "조회 종료년도는 yyyy 형식이어야 합니다.")
    String toYear
) {
}
