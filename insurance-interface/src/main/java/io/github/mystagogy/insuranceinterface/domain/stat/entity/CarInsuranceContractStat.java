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

@Entity
@Table(
    name = "car_insurance_contract_stat",
    indexes = {
        @Index(name = "idx_car_ins_contract_base_ym", columnList = "base_ym"),
        @Index(name = "idx_car_ins_contract_api_base_ym", columnList = "api_id, base_ym")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_car_ins_contract_stat_dedup",
            columnNames = {
                "base_ym",
                "insurance_type",
                "coverage_type",
                "gender",
                "age_group",
                "car_origin_type",
                "car_type",
                "api_id"
            }
        )
    }
)
public class CarInsuranceContractStat extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contract_stat_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "api_id", nullable = false)
    private ApiInfo apiInfo;

    @Column(name = "base_ym", nullable = false, length = 6)
    private String baseYm;

    @Column(name = "insurance_type", nullable = false, length = 50)
    private String insuranceType;

    @Column(name = "coverage_type", nullable = false, length = 50)
    private String coverageType;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 20)
    private GenderType gender;

    @Column(name = "age_group", nullable = false, length = 30)
    private String ageGroup;

    @Column(name = "car_origin_type", nullable = false, length = 30)
    private String carOriginType;

    @Column(name = "car_type", nullable = false, length = 50)
    private String carType;

    @Column(name = "contract_count", nullable = false)
    private long contractCount;

    @Column(name = "earned_premium", precision = 18, scale = 2)
    private BigDecimal earnedPremium;

    @Column(name = "raw_data", columnDefinition = "LONGTEXT")
    private String rawData;

    protected CarInsuranceContractStat() {
    }

    public CarInsuranceContractStat(
        ApiInfo apiInfo,
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
        this.apiInfo = apiInfo;
        this.baseYm = baseYm;
        this.insuranceType = insuranceType;
        this.coverageType = coverageType;
        this.gender = gender;
        this.ageGroup = ageGroup;
        this.carOriginType = carOriginType;
        this.carType = carType;
        this.contractCount = contractCount;
        this.earnedPremium = earnedPremium;
        this.rawData = rawData;
    }

    public Long getId() {
        return id;
    }

    public ApiInfo getApiInfo() {
        return apiInfo;
    }

    public String getBaseYm() {
        return baseYm;
    }

    public String getInsuranceType() {
        return insuranceType;
    }

    public String getCoverageType() {
        return coverageType;
    }

    public GenderType getGender() {
        return gender;
    }

    public String getAgeGroup() {
        return ageGroup;
    }

    public String getCarOriginType() {
        return carOriginType;
    }

    public String getCarType() {
        return carType;
    }

    public long getContractCount() {
        return contractCount;
    }

    public BigDecimal getEarnedPremium() {
        return earnedPremium;
    }

    public String getRawData() {
        return rawData;
    }
}
