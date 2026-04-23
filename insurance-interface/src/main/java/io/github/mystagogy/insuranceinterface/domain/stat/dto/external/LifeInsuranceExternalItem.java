package io.github.mystagogy.insuranceinterface.domain.stat.dto.external;

import io.github.mystagogy.insuranceinterface.domain.stat.entity.GenderType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record LifeInsuranceExternalItem(
    LocalDate statDate,
    String areaName,
    String ageGroup,
    GenderType gender,
    String insuranceType,
    long subscriptionCount,
    BigDecimal subscriptionRate,
    String rawData
) {
}
