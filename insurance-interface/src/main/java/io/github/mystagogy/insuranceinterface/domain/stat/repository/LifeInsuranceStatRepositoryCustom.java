package io.github.mystagogy.insuranceinterface.domain.stat.repository;

import io.github.mystagogy.insuranceinterface.domain.stat.entity.LifeInsuranceStat;
import java.util.List;

public interface LifeInsuranceStatRepositoryCustom {

    void upsertAll(List<LifeInsuranceStat> stats);
}
