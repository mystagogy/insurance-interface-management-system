package io.github.mystagogy.insuranceinterface.domain.stat.repository;

import io.github.mystagogy.insuranceinterface.domain.api.entity.ApiInfo;
import io.github.mystagogy.insuranceinterface.domain.stat.entity.LifeInsuranceStat;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LifeInsuranceStatRepository
    extends JpaRepository<LifeInsuranceStat, Long>, LifeInsuranceStatRepositoryCustom {

    @Query("""
        select s
        from LifeInsuranceStat s
        where s.apiInfo = :apiInfo
          and s.statDate between :fromDate and :toDate
        order by s.statDate asc, s.areaName asc, s.gender asc, s.ageGroup asc, s.insuranceType asc
        """)
    List<LifeInsuranceStat> findStats(
        @Param("apiInfo") ApiInfo apiInfo,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate
    );
}
