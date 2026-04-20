package io.github.mystagogy.insuranceinterface.domain.stat.entity;

import io.github.mystagogy.insuranceinterface.common.entity.BaseTimeEntity;
import io.github.mystagogy.insuranceinterface.domain.api.entity.ApiInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;

@Entity
@Table(
    name = "car_insurance_stat",
    indexes = {
        @Index(name = "idx_car_insurance_stat_stat_date", columnList = "stat_date")
    },
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_car_insurance_stat_dedup",
            columnNames = {"stat_date", "insurance_type", "damage_type", "api_id"}
        )
    }
)
public class CarInsuranceStat extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "api_id", nullable = false)
    private ApiInfo apiInfo;

    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;

    @Column(name = "insurance_type", nullable = false, length = 50)
    private String insuranceType;

    @Column(name = "damage_type", nullable = false, length = 50)
    private String damageType;

    @Column(name = "contract_count", nullable = false)
    private long contractCount;

    @Column(name = "accident_count", nullable = false)
    private long accidentCount;

    @Column(name = "victim_count", nullable = false)
    private long victimCount;

    @Column(name = "raw_data", columnDefinition = "LONGTEXT")
    private String rawData;

    protected CarInsuranceStat() {
    }

    public CarInsuranceStat(
        ApiInfo apiInfo,
        LocalDate statDate,
        String insuranceType,
        String damageType,
        long contractCount,
        long accidentCount,
        long victimCount,
        String rawData
    ) {
        this.apiInfo = apiInfo;
        this.statDate = statDate;
        this.insuranceType = insuranceType;
        this.damageType = damageType;
        this.contractCount = contractCount;
        this.accidentCount = accidentCount;
        this.victimCount = victimCount;
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

    public String getInsuranceType() {
        return insuranceType;
    }

    public String getDamageType() {
        return damageType;
    }

    public long getContractCount() {
        return contractCount;
    }

    public long getAccidentCount() {
        return accidentCount;
    }

    public long getVictimCount() {
        return victimCount;
    }

    public String getRawData() {
        return rawData;
    }
}
