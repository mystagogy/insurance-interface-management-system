package io.github.mystagogy.insuranceinterface.domain.stat.dto;

import java.util.List;

public record IndemnityInsuranceStatResponse(
    String fromYm,
    String toYm,
    int totalCount,
    List<IndemnityInsuranceStatItemResponse> items
) {
}
