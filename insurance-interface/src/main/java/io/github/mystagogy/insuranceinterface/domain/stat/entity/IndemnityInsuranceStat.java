package io.github.mystagogy.insuranceinterface.domain.stat.entity;

import io.github.mystagogy.insuranceinterface.common.entity.BaseTimeEntity;
import io.github.mystagogy.insuranceinterface.domain.api.entity.ApiInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
    name = "indemnity_insurance_stat",
    indexes = {
        @Index(name = "idx_indemnity_insurance_stat_stat_date", columnList = "stat_date")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_indemnity_insurance_stat_dedup",
            columnNames = {"stat_date", "age_group", "gender", "indemnity_type", "coverage_item", "api_id"}
        )
    }
)
public class IndemnityInsuranceStat extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "api_id", nullable = false)
    private ApiInfo apiInfo;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "age_group", nullable = false, length = 30)
    private String ageGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 20)
    private GenderType gender;

    @Column(name = "indemnity_type", nullable = false, length = 50)
    private String indemnityType;

    @Column(name = "coverage_item", nullable = false, length = 100)
    private String coverageItem;

    @Column(name = "premium_amount", precision = 15, scale = 2)
    private BigDecimal premiumAmount;

    @Column(name = "raw_data", columnDefinition = "LONGTEXT")
    private String rawData;

    protected IndemnityInsuranceStat() {
    }

    public IndemnityInsuranceStat(
        ApiInfo apiInfo,
        LocalDate statDate,
        String ageGroup,
        GenderType gender,
        String indemnityType,
        String coverageItem,
        BigDecimal premiumAmount,
        String rawData
    ) {
        this.apiInfo = apiInfo;
        this.statDate = statDate;
        this.ageGroup = ageGroup;
        this.gender = gender;
        this.indemnityType = indemnityType;
        this.coverageItem = coverageItem;
        this.premiumAmount = premiumAmount;
        this.rawData = rawData;
    }

    public Long getId() {
        return id;
    }

    public ApiInfo getApiInfo() {
        return apiInfo;
    }

    public LocalDate getStatDate() {
        return statDate;
    }

    public String getAgeGroup() {
        return ageGroup;
    }

    public GenderType getGender() {
        return gender;
    }

    public String getIndemnityType() {
        return indemnityType;
    }

    public String getCoverageItem() {
        return coverageItem;
    }

    public BigDecimal getPremiumAmount() {
        return premiumAmount;
    }

    public String getRawData() {
        return rawData;
    }
}
