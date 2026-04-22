package io.github.mystagogy.insuranceinterface.domain.stat.dto;

import java.util.List;

public record CarInsuranceContractStatResponse(
    String fromYm,
    String toYm,
    int totalCount,
    List<CarInsuranceContractStatItemResponse> items
) {
}
