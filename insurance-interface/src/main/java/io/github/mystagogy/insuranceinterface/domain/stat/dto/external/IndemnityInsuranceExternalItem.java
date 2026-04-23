package io.github.mystagogy.insuranceinterface.domain.stat.dto.external;

import io.github.mystagogy.insuranceinterface.domain.stat.entity.GenderType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record IndemnityInsuranceExternalItem(
    LocalDate statDate,
    String ageGroup,
    GenderType gender,
    String indemnityType,
    String coverageItem,
    BigDecimal premiumAmount,
    String rawData
) {
}
