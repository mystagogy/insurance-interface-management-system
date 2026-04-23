package io.github.mystagogy.insuranceinterface.domain.stat.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LifeInsuranceStatItemResponse(
    LocalDate statDate,
    String areaName,
    String ageGroup,
    String gender,
    String insuranceType,
    long subscriptionCount,
    BigDecimal subscriptionRate
) {
}
