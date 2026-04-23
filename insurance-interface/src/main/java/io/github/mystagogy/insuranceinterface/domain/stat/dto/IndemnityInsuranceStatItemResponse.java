package io.github.mystagogy.insuranceinterface.domain.stat.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IndemnityInsuranceStatItemResponse(
    LocalDate statDate,
    String ageGroup,
    String gender,
    String indemnityType,
    String coverageItem,
    BigDecimal premiumAmount
) {
}
