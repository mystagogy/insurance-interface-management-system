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
    name = "life_insurance_stat",
    indexes = {
        @Index(name = "idx_life_insurance_stat_stat_date", columnList = "stat_date"),
        @Index(
            name = "idx_life_insurance_stat_search",
            columnList = "api_id, stat_date, area_name, gender, age_group, insurance_type"
        )
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_life_insurance_stat_dedup",
            columnNames = {"stat_date", "area_name", "age_group", "gender", "insurance_type", "api_id"}
        )
    }
)
public class LifeInsuranceStat extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "api_id", nullable = false)
    private ApiInfo apiInfo;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "area_name", nullable = false, length = 50)
    private String areaName;

    @Column(name = "age_group", nullable = false, length = 30)
    private String ageGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 20)
    private GenderType gender;

    @Column(name = "insurance_type", nullable = false, length = 50)
    private String insuranceType;

    @Column(name = "subscription_count", nullable = false)
    private long subscriptionCount;

    @Column(name = "subscription_rate", precision = 7, scale = 4)
    private BigDecimal subscriptionRate;

    @Column(name = "raw_data", columnDefinition = "LONGTEXT")
    private String rawData;

    protected LifeInsuranceStat() {
    }

    public LifeInsuranceStat(
        ApiInfo apiInfo,
        LocalDate statDate,
        String areaName,
        String ageGroup,
        GenderType gender,
        String insuranceType,
        long subscriptionCount,
        BigDecimal subscriptionRate,
        String rawData
    ) {
        this.apiInfo = apiInfo;
        this.statDate = statDate;
        this.areaName = areaName;
        this.ageGroup = ageGroup;
        this.gender = gender;
        this.insuranceType = insuranceType;
        this.subscriptionCount = subscriptionCount;
        this.subscriptionRate = subscriptionRate;
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

    public String getAreaName() {
        return areaName;
    }

    public String getAgeGroup() {
        return ageGroup;
    }

    public GenderType getGender() {
        return gender;
    }

    public String getInsuranceType() {
        return insuranceType;
    }

    public long getSubscriptionCount() {
        return subscriptionCount;
    }

    public BigDecimal getSubscriptionRate() {
        return subscriptionRate;
    }

    public String getRawData() {
        return rawData;
    }
}
