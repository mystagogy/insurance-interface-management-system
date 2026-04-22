package io.github.mystagogy.insuranceinterface.domain.stat.repository;

import io.github.mystagogy.insuranceinterface.domain.stat.entity.CarInsuranceContractStat;
import java.util.List;

public interface CarInsuranceContractStatRepositoryCustom {

    void upsertAll(List<CarInsuranceContractStat> stats);
}
