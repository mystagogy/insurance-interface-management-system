package io.github.mystagogy.insuranceinterface.domain.stat.repository;

import io.github.mystagogy.insuranceinterface.domain.stat.entity.IndemnityInsuranceStat;
import java.util.List;

public interface IndemnityInsuranceStatRepositoryCustom {

    void upsertAll(List<IndemnityInsuranceStat> stats);
}
