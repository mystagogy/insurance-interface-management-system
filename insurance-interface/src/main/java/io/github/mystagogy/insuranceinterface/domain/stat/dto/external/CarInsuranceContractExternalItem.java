package io.github.mystagogy.insuranceinterface.domain.stat.dto.external;

import io.github.mystagogy.insuranceinterface.domain.stat.entity.GenderType;
import java.math.BigDecimal;

public record CarInsuranceContractExternalItem(
    String baseYm,
    String insuranceType,
    String coverageType,
    GenderType gender,
    String ageGroup,
    String carOriginType,
    String carType,
    long contractCount,
    BigDecimal earnedPremium,
    String rawData
) {
}
