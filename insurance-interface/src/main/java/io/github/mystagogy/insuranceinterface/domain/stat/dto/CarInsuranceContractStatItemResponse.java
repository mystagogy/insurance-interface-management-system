package io.github.mystagogy.insuranceinterface.domain.stat.dto;

import java.math.BigDecimal;

public record CarInsuranceContractStatItemResponse(
    String baseYm,
    String insuranceType,
    String coverageType,
    String gender,
    String ageGroup,
    String carOriginType,
    String carType,
    long contractCount,
    BigDecimal earnedPremium
) {
}
