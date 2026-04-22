package io.github.mystagogy.insuranceinterface.domain.stat.repository;

import io.github.mystagogy.insuranceinterface.domain.stat.entity.CarInsuranceContractStat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarInsuranceContractStatRepository
    extends JpaRepository<CarInsuranceContractStat, Long>, CarInsuranceContractStatRepositoryCustom {

    List<CarInsuranceContractStat> findByBaseYmBetweenOrderByBaseYmAsc(String fromYm, String toYm);
}
