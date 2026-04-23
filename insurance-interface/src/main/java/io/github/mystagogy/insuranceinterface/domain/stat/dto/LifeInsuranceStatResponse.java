package io.github.mystagogy.insuranceinterface.domain.stat.dto;

import java.util.List;

public record LifeInsuranceStatResponse(
    String fromYear,
    String toYear,
    int totalCount,
    List<LifeInsuranceStatItemResponse> items
) {
}
